import { buildApp } from './app.js';
import { connectRabbitPublisher } from './amqp/publisher.js';
import { loadConfig } from './config.js';

const config = loadConfig();
const publisher = await connectRabbitPublisher(config.rabbitUrl);
const app = await buildApp({ config, publisher });
app.addHook('onClose', async () => {
  await publisher.close();
});
await app.listen({ port: config.port, host: config.host });
