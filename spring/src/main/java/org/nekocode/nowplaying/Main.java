package org.nekocode.nowplaying;

import org.nekocode.nowplaying.remote.mediamonkey5.MM5RemoteModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

@Import({
        NowPlayingController.class,
        MM5RemoteModel.class,
})
public class Main implements CommandLineRunner {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class)
                .headless(false)
                .run(args);
    }

    @Autowired
    private NowPlayingController controller;

    public void run(String... args)
    {
        LogMuter.muteLogging();
        controller.start();
    }
}
