package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.EnableScalingProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.UtilizationScalingProps;
import software.constructs.Construct;

public class DynamoDBStack extends Stack {

    private final Table productEventsDdb;

    public DynamoDBStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DynamoDBStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Criação da Estrutura Básica da Tabela a ser criada no DynamoDB
        productEventsDdb = Table.Builder.create(this, "ProductEventsDdb")
                .tableName("product-events")
                .billingMode(BillingMode.PROVISIONED) // Tipo de Cobrança por acesso a tabela no DynamoDB
                .readCapacity(1) // capacidade de leitura e escrita por segundo na Tabela do DynamoDB
                .writeCapacity(1)
                .partitionKey(Attribute.builder()
                        .name("pk")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute("ttl") // tempo
                .removalPolicy(RemovalPolicy.DESTROY) // Ao apagar a Stack devera destruir a Tabela criada no DynamoDB
                .build();

        // Politica de Scaling Provisionade
        // Configurando Scaling para adequar a capacidade de requisições esperadas.
        // Leitura
        productEventsDdb.autoScaleReadCapacity(EnableScalingProps.builder()
                        .minCapacity(1)
                        .maxCapacity(4)
                        .build())
                .scaleOnUtilization(UtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(30))
                        .scaleOutCooldown(Duration.seconds(30))
                        .build());
        // Escrita
        productEventsDdb.autoScaleWriteCapacity(EnableScalingProps.builder()
                        .minCapacity(1)
                        .maxCapacity(4)
                        .build())
                .scaleOnUtilization(UtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(30))
                        .scaleOutCooldown(Duration.seconds(30))
                        .build());
    }

    public Table getProductEventsDdb() {
        return productEventsDdb;
    }
}
