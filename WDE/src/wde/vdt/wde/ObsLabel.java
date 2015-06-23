package wde.vdt.wde;


class ObsLabel implements Comparable<ObsLabel> {
    protected String m_sTypeName;
    float m_fValue;


    private ObsLabel() {
    }


    ObsLabel(CharSequence sTypeName, float fValue) {
        m_sTypeName = sTypeName.toString().intern();
        m_fValue = fValue;
    }


    @Override
    public int compareTo(ObsLabel oObsLabel) {
        return m_sTypeName.compareTo(oObsLabel.m_sTypeName);
    }
}
