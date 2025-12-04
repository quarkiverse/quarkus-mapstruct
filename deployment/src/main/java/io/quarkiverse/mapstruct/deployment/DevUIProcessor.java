package io.quarkiverse.mapstruct.deployment;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;

public class DevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void createCard(BuildProducer<CardPageBuildItem> cardPageBuildItemBuildProducer) {
        final CardPageBuildItem card = new CardPageBuildItem();

        card.addLibraryVersion("org.mapstruct", "mapstruct", "Mapstruct",
                "https://mapstruct.org/documentation/stable/reference/html/");

        cardPageBuildItemBuildProducer.produce(card);
    }

}
