package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class Service02Stack extends Stack {
    public Service02Stack(final Construct scope, final String id, Cluster cluster, SnsTopic productEventsTopic, Table productEventDynamoDB) {
        this(scope, id, null, cluster, productEventsTopic, productEventDynamoDB);
    }

    public Service02Stack(final Construct scope, final String id, final StackProps props, Cluster cluster, SnsTopic productEventsTopic, Table productEventDynamoDB) {
        super(scope, id, props);

        // fila de DLQ - recebe as mensagens que não foram tratadas pelo consumidor
        Queue productEventsDlq = Queue.Builder.create(this, "ProductEventsDlq")
                .queueName("product-events-dlq")
                .build();

        // Criando a Fila DLQ e associando a representação da DLQ
        DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
                .queue(productEventsDlq)
                .maxReceiveCount(3)
                .build();

        // Criando a fila principal que irá de fato receber as mensagens
        Queue productEventsQueue = Queue.Builder.create(this, "ProductEvents")
                .queueName("product-events")
                .deadLetterQueue(deadLetterQueue)
                .build();

        // inscrevendo a fila principal dentro do nosso tópico

        SqsSubscription sqsSubscription = SqsSubscription.Builder.create(productEventsQueue).build();
        productEventsTopic.getTopic().addSubscription(sqsSubscription);

        // mesma configuração a ser criada no application.properties
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("AWS_REGION", "us-east-1");
        envVariables.put("AWS_SQS_QUEUE_PRODUCT_EVENTS_NAME", productEventsQueue.getQueueName());

        ApplicationLoadBalancedFargateService service02 = ApplicationLoadBalancedFargateService.Builder.create(this, "ALB02")
                .serviceName("service-02")
                .cluster(cluster)
                .cpu(512)
                .memoryLimitMiB(1024)
                .desiredCount(2)
                .listenerPort(9090)
                .assignPublicIp(true) // seguindo a regra de não permitir a criação do natGateways(0) definido no recurso de criação da Vpc

                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("aws_project02")
                                .image(ContainerImage.fromRegistry("julianfernando/curso_aws_project02:1.3.0")) // o name deverá ser obtido no dockerhub
                                .containerPort(9090)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this, "Service02LogGroup")
                                                .logGroupName("Service02")
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())
                                        .streamPrefix("Service02")
                                        .build()))
                                .environment(envVariables) // definindo as varáveis de ambiente na Task de Serviço
                                .build())
                .publicLoadBalancer(true)
                .build();

        // configuração de Health Check recurso do Load balancer
        service02.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                .path("/actuator/health")
                .port("9090")
                .healthyHttpCodes("200")
                .build());

        ScalableTaskCount scalableTaskCount = service02.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2) // minimo de instancias sugerido
                .maxCapacity(4) // maximo de instancias sugerido
                .build());

        scalableTaskCount.scaleOnCpuUtilization("Service02AutoScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());

        // permitindo que o serviço consuma mensagens da fila adicionando permissões
        productEventsQueue.grantConsumeMessages(service02.getTaskDefinition().getTaskRole());

        // permissões de leitura e escrita para que o serviço 02 possa acessar a tabela DynamoDB.
        productEventDynamoDB.grantReadWriteData(service02.getTaskDefinition().getTaskRole());
    }
}
