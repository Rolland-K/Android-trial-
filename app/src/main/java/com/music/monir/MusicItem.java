package com.music.monir;



import com.google.firebase.database.IgnoreExtraProperties;

// or @ThrowOnExtraProperties
@IgnoreExtraProperties
public class MusicItem {

    private String instrument = "";
    private String beats = "";
    private String bpm = "";
    private String song = "";
    private String pitch = "";
    private String lr = "";
    private String url = "";

    public MusicItem() {
        // empty for Firebase
    }

    public String getInstrument() {
        return  instrument;
    }

    public String getBpm() {
        return  bpm;
    }

    public String getSong() {
        return  song;
    }

    public String getPitch() {
        return  pitch;
    }

    public String getLr() {
        return  lr;
    }

    public String getUrl() {
        return  url;
    }

    public void setBeats(String beats) {
        this.beats = beats;
    }

    public String getBeats() {
        return beats;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    @Override
    public String toString() {
        if(getLr().compareTo("LR") == 0)
            return getInstrument() + "_" + getBeats() + "_" + getSong() + "_" + getBpm() + "_" + getPitch() + "_" + getLr() + ".mp3";
        else
            return getInstrument() + "_" + getBeats() + "_" + getSong() + "_" + getBpm() + "_" + getPitch()  + ".mp3";

    }

    // implement for easy comparison
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof MusicItem) {
            MusicItem item = ((MusicItem)obj);
            if (item.getInstrument() == null) {
                return false;
            }
            return item.getInstrument().contentEquals(getInstrument());
        }

        return super.equals(obj);
    }
}