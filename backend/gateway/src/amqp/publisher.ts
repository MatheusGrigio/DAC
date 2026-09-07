import { connect } from 'amqplib';
import type { MessageEnvelope } from '../types/envelopes.js';
import { QueueNames } from '../types/queues.js';

export type SagaPublisher = {
  publish(envelope: MessageEnvelope): Promise<void>;
};

export const noopSagaPublisher: SagaPublisher = {
  async publish() {
    return;
  },
};

export async function connectRabbitPublisher(
  url: string,
): Promise<SagaPublisher & { close(): Promise<void> }> {
  const connection = await connect(url);
  const channel = await connection.createChannel();
  await channel.assertQueue(QueueNames.SAGA_CMD, { durable: true });
  return {
    async publish(envelope: MessageEnvelope) {
      channel.sendToQueue(QueueNames.SAGA_CMD, Buffer.from(JSON.stringify(envelope)), {
        persistent: true,
        contentType: 'application/json',
      });
    },
    async close() {
      await channel.close();
      await connection.close();
    },
  };
}
