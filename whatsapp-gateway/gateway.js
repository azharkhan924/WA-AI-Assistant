const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcodeTerminal = require('qrcode-terminal');
const qrcode = require('qrcode');
const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const axios = require('axios');
const path = require('path');
const cors = require('cors');

const PORT = process.env.PORT || 8083;
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8082/api/webhook/whatsapp';

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

let client = null;
let isReady = false;
let lastQr = null;
let lastQrDataUrl = null;
let isReconnecting = false;

function getDashboardStatus() {
  if (isReady && client && client.info) {
    const rawNum = client.info.wid ? client.info.wid.user : null;
    return {
      status: 'connected',
      connectedNumber: rawNum,
    };
  }
  if (lastQrDataUrl && !isReady) {
    return {
      status: 'qr',
      qrDataUrl: lastQrDataUrl,
    };
  }
  if (isReconnecting) {
    return {
      status: 'initializing',
    };
  }
  return {
    status: isReady ? 'connected' : 'disconnected',
  };
}

function broadcastState() {
  const status = getDashboardStatus();
  io.emit('state', status);
  console.log(`[Gateway State]: ${status.status} ${status.connectedNumber ? '(+' + status.connectedNumber + ')' : ''}`);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function computeHumanDelay(replyLength = 0, incomingLength = 0) {
  const baseJitter = 2000 + Math.floor(Math.random() * 2000);
  const lengthBonus = Math.min(replyLength * 25 + incomingLength * 10, 10000);
  return baseJitter + lengthBonus;
}

function createClient() {
  console.log('[Gateway]: Creating WhatsApp Web client...');
  client = new Client({
    authStrategy: new LocalAuth({
      clientId: 'whatsapp-gateway-session',
      dataPath: path.join(__dirname, 'session')
    }),
    puppeteer: {
      headless: true,
      executablePath: process.env.PUPPETEER_EXECUTABLE_PATH || undefined,
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',
        '--disable-gpu',
        '--disable-features=IsolateOrigins,site-per-process',
        '--disable-site-isolation-trials',
      ]
    }
  });

  client.on('qr', async (qr) => {
    lastQr = qr;
    console.log('[Gateway]: New QR generated! Scan via WhatsApp Linked Devices.');
    qrcodeTerminal.generate(qr, { small: true });
    try {
      lastQrDataUrl = await qrcode.toDataURL(qr);
    } catch (err) {
      console.error('[Gateway]: Failed to generate QR data URL:', err.message);
    }
    broadcastState();
  });

  client.on('authenticated', () => {
    console.log('[Gateway]: Authenticated successfully.');
    broadcastState();
  });

  client.on('ready', () => {
    isReady = true;
    lastQr = null;
    lastQrDataUrl = null;
    console.log('[Gateway]: WhatsApp client is fully ready.');
    broadcastState();
  });

  client.on('disconnected', async (reason) => {
    console.log(`[Gateway]: Client was disconnected: ${reason}`);
    isReady = false;
    lastQrDataUrl = null;
    broadcastState();
    reconnectClient('Disconnected event from WhatsApp Web');
  });

  client.on('message', async (message) => {
    // Ignore group chats, broadcast messages, status, and messages from self
    if (message.fromMe || message.isStatus || message.from.includes('@g.us') || message.from === 'status@broadcast') {
      return;
    }

    const text = (message.body || '').trim();
    if (!text) return;

    console.log(`[Gateway]: Incoming message from ${message.from}: "${text}"`);

    // Prepare Twilio emulation payload
    const cleanFrom = message.from.replace(/[^0-9]/g, '');
    const cleanTo = client.info && client.info.wid ? client.info.wid.user : 'bot';

    const params = new URLSearchParams();
    params.append('From', `whatsapp:+${cleanFrom}`);
    params.append('To', `whatsapp:+${cleanTo}`);
    params.append('Body', text);

    try {
      // Forward to Spring Boot backend
      const res = await axios.post(BACKEND_URL, params.toString(), {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        }
      });

      // Parse TwiML response
      const xmlData = res.data;
      const match = xmlData.match(/<Message>([\s\S]*?)<\/Message>/);
      let reply = match ? match[1].trim() : '';
      
      if (reply) {
        // Decode XML entities
        reply = reply
          .replace(/&amp;/g, '&')
          .replace(/&lt;/g, '<')
          .replace(/&gt;/g, '>')
          .replace(/&quot;/g, '"')
          .replace(/&apos;/g, "'");

        // Human delay
        if (typeof message.getChat === 'function') {
          const chat = await message.getChat();
          if (chat && typeof chat.sendStateTyping === 'function') {
            await chat.sendStateTyping();
          }
        }
        
        const delay = computeHumanDelay(reply.length, text.length);
        console.log(`[Gateway]: Generating reply with ${Math.round(delay)}ms human delay...`);
        await sleep(delay);

        await message.reply(reply);
        console.log(`[Gateway]: Replied successfully to ${message.from}`);
      }
    } catch (err) {
      console.error('[Gateway]: Failed to forward message or send reply:', err.message);
    }
  });

  client.initialize().catch((err) => {
    console.error('[Gateway]: Initialization error:', err.message);
  });
}

async function reconnectClient(reason) {
  if (isReconnecting) return;
  isReconnecting = true;
  isReady = false;
  lastQrDataUrl = null;
  console.log(`[Gateway Reconnect]: Triggering reconnect. Reason: ${reason}`);
  broadcastState();

  try {
    if (client) {
      await client.destroy();
    }
  } catch (err) {
    console.error('[Gateway Reconnect]: Error destroying client:', err.message);
  }

  client = null;
  await sleep(3000);
  isReconnecting = false;
  createClient();
}

// REST Endpoints
app.post('/api/whatsapp/sendMessage', async (req, res) => {
  const { to, text } = req.body;
  if (!to || !text) {
    return res.status(400).json({ error: 'Missing required parameters: to and text' });
  }

  if (!isReady || !client) {
    return res.status(503).json({ error: 'WhatsApp client is not connected' });
  }

  let target = to.replace(/[^0-9]/g, '');
  if (!target.endsWith('@c.us') && !target.endsWith('@g.us')) {
    target = `${target}@c.us`;
  }

  try {
    await client.sendMessage(target, text);
    console.log(`[Gateway API]: Sent outgoing message to ${target}`);
    return res.json({ success: true });
  } catch (err) {
    console.error('[Gateway API]: Failed to send message:', err.message);
    return res.status(500).json({ error: err.message });
  }
});

app.post('/api/whatsapp/reconnect', async (req, res) => {
  reconnectClient('Triggered manually via HTTP API');
  return res.json({ success: true, message: 'Reconnection triggered' });
});

app.get('/api/whatsapp/status', (req, res) => {
  return res.json(getDashboardStatus());
});

// Socket connection
io.on('connection', (socket) => {
  console.log(`[Gateway Socket]: Dashboard client connected (${socket.id})`);
  socket.emit('state', getDashboardStatus());
  
  socket.on('disconnect', () => {
    console.log(`[Gateway Socket]: Dashboard client disconnected (${socket.id})`);
  });
});

server.listen(PORT, () => {
  console.log(`[Gateway]: Server running on port ${PORT}`);
  createClient();
});
