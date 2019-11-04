package com.music.monir;



import com.google.firebase.database.IgnoreExtraProperties;

// or @ThrowOnExtraProperties
@IgnoreExtraProperties
public class TanpuraItem {

    private String instrument = "";
    private String pitch = "";
    private String url = "";

    public TanpuraItem() {
        // empty for Firebase
    }

    public String getInstrument() {
        return  instrument;
    }


    public String getPitch() {
        return  pitch;
    }

    public String getUrl() {
        return  url;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    @Override
    public String toString() {
        return getInstrument() + "_" + getPitch()  + ".mp3";

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