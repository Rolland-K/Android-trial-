package com.music.monir;



import com.google.firebase.database.IgnoreExtraProperties;

// or @ThrowOnExtraProperties
@IgnoreExtraProperties
public class InfoItem {

    private String information = "";
    private String url = "";

    public InfoItem() {
        // empty for Firebase
    }

    public String getInformation() {
        return  information;
    }


    public String getUrl() {
        return  url;
    }

    public void setInformation(String instrument) {
        this.information = instrument;
    }

    @Override
    public String toString() {
        return getInformation() ;

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
            return item.getInstrument().contentEquals(getInformation());
        }

        return super.equals(obj);
    }
}