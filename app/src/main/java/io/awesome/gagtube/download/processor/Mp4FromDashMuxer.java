package io.awesome.gagtube.download.processor;

import java.io.IOException;

import io.awesome.gagtube.streams.Mp4FromDashWriter;
import io.awesome.gagtube.streams.io.SharpStream;

class Mp4FromDashMuxer extends PostProcessing {
	
	Mp4FromDashMuxer() {
		super(true, true, ALGORITHM_MP4_FROM_DASH_MUXER);
	}
	
	@Override
	int process(SharpStream out, SharpStream... sources) throws IOException {
		Mp4FromDashWriter muxer = new Mp4FromDashWriter(sources);
		muxer.parseSources();
		muxer.selectTracks(0, 0);
		muxer.build(out);
		
		return OK_RESULT;
	}
	
}
