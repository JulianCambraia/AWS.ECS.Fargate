package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.constructs.Construct;

public class SnsStack extends Stack {

    private final SnsTopic productEventsTopic;

    public SnsStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SnsStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        productEventsTopic = SnsTopic.Builder.create(Topic.Builder.create(this, "productEventsTopic")
                        .topicName("product-events")
                        .build())
                .build();

        // inscrever o E-mail no Topic do Sns
        productEventsTopic.getTopic().addSubscription(EmailSubscription.Builder.create("juliancambraia@gmail.com")
                .json(true)
                .build());
    }

    public SnsTopic getProductEventsTopic() {
        return productEventsTopic;
    }
}
