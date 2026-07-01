FROM ghcr.io/puppeteer/puppeteer:21.5.0

# Set environment variables for Puppeteer
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/google-chrome-stable

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy the rest of the application code
COPY . .

# Render injects PORT env variable, gateway.js will bind to it
EXPOSE 8083

CMD ["node", "gateway.js"]
