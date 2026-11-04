package com.andrerinas.openheadunit.decoder.audio

import org.junit.Assert.*
import org.junit.Test

class FiorinoDuckingTest {
    @Test fun `live level changes restore fully after speech`() {
        var level = .3f
        val envelope = MixerDuckingEnvelope(speechGain = { level })
        envelope.setSpeechActive(true)
        repeat(480) { envelope.nextGain() }
        assertEquals(.3f, envelope.nextGain(), 0f)
        level = 0f
        envelope.setSpeechActive(true)
        repeat(480) { envelope.nextGain() }
        assertEquals(0f, envelope.nextGain(), 0f)
        envelope.setSpeechActive(false)
        repeat(5760) { envelope.nextGain() }
        assertEquals(1f, envelope.nextGain(), 0f)
        assertTrue(MixerDuckingEnvelope(speechGain = { level }).isSettledUnity)
    }

    @Test fun `unused navigation channel and missing stop cannot strand music gain`() {
        val mixer = AudioMixer(duckMediaForSpeech = true, speechGain = { .3f })
        val music = mixer.registerChannel(4, 48000, 2, 1, true)
        val guidance = mixer.registerChannel(5, 48000, 2, 1, false)
        var last = shortArrayOf()
        repeat(150) { cycle ->
            val now = cycle * 10L
            val pcm = ShortArray(960) { 10000 }
            music.buffer.noteArrival(now, 480)
            music.buffer.write(pcm, pcm.size, now)
            if (cycle in 20..29) {
                guidance.buffer.noteArrival(now, 480)
                guidance.buffer.write(ShortArray(960) { 500 }, 960, now)
            }
            // No finish event: a phone can lose the stop notification.
            last = mixer.renderCycle(now, 480)
            if (cycle == 19) assertEquals(10000, last.last().toInt())
        }
        assertEquals(10000, last.last().toInt())
        assertFalse(guidance.activeThisCycle)
    }
}
