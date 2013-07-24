/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.jstakun.gms.android.ui.lib.R;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class IntentArrayAdapter extends ArrayAdapter<ResolveInfo> {
    private PackageManager pm = null;
    private Activity context;

    public IntentArrayAdapter(Activity context, List<ResolveInfo> apps) {
      super(context, R.layout.intentrow, apps);
      this.context = context;
      this.pm = context.getPackageManager();
    }

    @Override
    public View getView(int position, View convertView,
                          ViewGroup parent) {

      if (convertView == null) {
        convertView = newView(parent);
      }

      bindView(position, convertView);

      return(convertView);
    }

    private View newView(ViewGroup parent) {
      return(context.getLayoutInflater().inflate(R.layout.intentrow, parent, false));
    }

    private void bindView(int position, View row) {
      TextView label=(TextView)row.findViewById(R.id.intentLabel);

      label.setText(getItem(position).loadLabel(pm));

      ImageView icon=(ImageView)row.findViewById(R.id.intentIcon);

      icon.setImageDrawable(getItem(position).loadIcon(pm));
    }
  }
