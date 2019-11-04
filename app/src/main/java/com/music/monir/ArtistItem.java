package com.music.monir;



import com.google.firebase.database.IgnoreExtraProperties;

// or @ThrowOnExtraProperties
@IgnoreExtraProperties
public class ArtistItem {

    private String name = "";
    private String detail = "";
    private String url = "";

    public ArtistItem() {
        // empty for Firebase
    }

    public String getName() {
        return  name;
    }


    public String getDetail() {
        return  detail;
    }

    public String getUrl() {
        return  url;
    }



    @Override
    public String toString() {
        return getName();

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
            return item.getInstrument().contentEquals(getName());
        }

        return super.equals(obj);
    }
}