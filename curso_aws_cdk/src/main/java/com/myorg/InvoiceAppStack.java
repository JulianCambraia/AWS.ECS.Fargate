package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.s3.notifications.SnsDestination;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

public class InvoiceAppStack extends Stack {

    private final Bucket bucket;

    private final Queue s3InvoiceQueue;

    public InvoiceAppStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InvoiceAppStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        SnsTopic s3InvoiceTopic = SnsTopic.Builder.create(Topic.Builder.create(this, "S3InvoiceTopic")
                        .topicName("s3-invoice-events")
                        .build())
                .build();

        bucket = Bucket.Builder.create(this, "S301")
                .bucketName("pcs01-invoice")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // gerando notificação quando inserir objeto no bucket
        bucket.addEventNotification(EventType.OBJECT_CREATED_PUT, new SnsDestination(s3InvoiceTopic.getTopic()));

        // DLQ
        Queue s3InvoiceDlq = Queue.Builder.create(this, "S3InvoiceDlq")
                .queueName("s3-invoice-events-dlq")
                .build();

        DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
                .queue(s3InvoiceDlq)
                .maxReceiveCount(3)
                .build();

        s3InvoiceQueue = Queue.Builder.create(this, "S3InvoiceQueue")
                .queueName("s3-invoice-events")
                .deadLetterQueue(deadLetterQueue)
                .build();

        SqsSubscription sqsSubscription = SqsSubscription.Builder.create(s3InvoiceQueue).build();
        s3InvoiceTopic.getTopic().addSubscription(sqsSubscription);
    }

    public Bucket getBucket() {
        return bucket;
    }

    public Queue getS3InvoiceQueue() {
        return s3InvoiceQueue;
    }
}
