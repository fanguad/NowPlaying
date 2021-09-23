package org.nekocode.nowplaying;

import org.nekocode.nowplaying.components.NowPlayingControl;
import org.nekocode.nowplaying.components.modes.control.ControlPanel;
import org.nekocode.nowplaying.components.modes.tag.TagPanel;
import org.nekocode.nowplaying.components.modes.tagsdnd.TagDnDPanel;
import org.nekocode.nowplaying.internals.TrackMonitor;
import org.nekocode.nowplaying.remote.mediamonkey5.MM5RemoteModel;
import org.nekocode.nowplaying.tags.TagModel;
import org.nekocode.nowplaying.tags.TagView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

@Import({
        NowPlayingController.class,
        MM5RemoteModel.class,

        NowPlayingView.class,
        TagView.class,

        TagModel.class,
        TrackMonitor.class,

        ControlPanel.class,
        TagPanel.class,
        TagDnDPanel.class,
})
public class Main implements CommandLineRunner {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class)
                .headless(false)
                .run(args);
    }

    /**
     * This bean primarily exists to put these modes in the correct order
     */
    @Bean
    @Qualifier("modes")
    public List<NowPlayingControl> modes(ControlPanel controlPanel,
                                         TagPanel tagPanel,
                                         TagDnDPanel tagDnDPanel)
    {
        return List.of(controlPanel, tagPanel, tagDnDPanel);
    }

    @Autowired
    private NowPlayingController controller;

    public void run(String... args)
    {
        LogMuter.muteLogging();
        controller.start();
    }
}
