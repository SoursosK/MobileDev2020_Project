package icsd.corpa;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class Defibrillator implements ClusterItem {

    private final String nickname;
    private final String loc_desc;
    private final LatLng latLng;

    public Defibrillator(String nickname, String loc_desc, LatLng latLng) {
        this.nickname = nickname;
        this.loc_desc = loc_desc;
        this.latLng = latLng;
    }

    @Override
    public LatLng getPosition() {
        return latLng;
    }

    @Override
    public String getTitle() {
        return nickname;
    }

    @Override
    public String getSnippet() {
        return loc_desc;
    }
}
