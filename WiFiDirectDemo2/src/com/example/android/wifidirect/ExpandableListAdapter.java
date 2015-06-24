package com.example.android.wifidirect;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by AT e DR on 24-06-2015.
 */
public class ExpandableListAdapter<L1Type, L2Type> extends BaseExpandableListAdapter {
    private Context _context;
    private List<L1Type> _listDataHeader; // header titles
    // child data in format of header title, child title
    private HashMap<L1Type, List<L2Type>> _listDataChild;

    public ExpandableListAdapter(Context context, List<L1Type> listDataHeader,
                                 HashMap<L1Type, List<L2Type>> listChildData) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
    }

    public ExpandableListAdapter(Context context) {
        this._context = context;
        this._listDataHeader = new ArrayList<>();
        this._listDataChild = new HashMap<>();
    }



    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = getChild(groupPosition, childPosition).toString();

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.lblListItem);

        txtListChild.setText(childText);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        List<L2Type> level2List = this._listDataChild.get(this._listDataHeader.get(groupPosition));
        return (level2List != null) ? level2List.size() : 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = getGroup(groupPosition).toString();

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void addDataHeader(L1Type elem){
        _listDataHeader.add(elem);
        this.notifyDataSetChanged();
    }

    public void addDataHeaderAndChildren(L1Type elem, List<L2Type> childList){
        _listDataHeader.add(elem);
        _listDataChild.put(elem, childList);
        this.notifyDataSetChanged();
    }

    public void addDataChild(L1Type elem, L2Type child){
        if(_listDataChild.containsKey(elem))
            _listDataChild.get(elem).add(child);
        else{
            List <L2Type> al = new ArrayList<>();
            al.add(child);
            addDataHeaderAndChildren(elem, al);
        }
    }

    public void clear() {
        _listDataHeader.clear();
        _listDataChild.clear();
        this.notifyDataSetChanged();
    }
}
