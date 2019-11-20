package com.lt.lrmd.lhfa;

import android.content.Context;
import android.database.MatrixCursor;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;

public class SearchSuggestionHelper {

    public SearchSuggestionHelper()
    {
    }

    public SimpleCursorAdapter buildSuggestionAdapter(Context context, ArrayList<Placemark> placemarks, String searchText)
    {
        String[] columnNames = {"_id", "text"};
        MatrixCursor cursor = new MatrixCursor(columnNames);
        String[] strings = new String[2];
        if (searchText == null)
        {
            for (int i = 0; i < placemarks.size(); i++)
            {
                Placemark placemark = placemarks.get(i);
                strings[0] = Integer.toString(i);
                strings[1] = placemark.getName() + ", " + placemark.getDescription();
                cursor.addRow(strings);
            }
        } else
        {
            int k = 0;
            for (int i = 0; i < placemarks.size(); i++, k++)
            {
                Placemark placemark = placemarks.get(i);
                String placemarkName = placemark.getName() + ", " + placemark.getDescription();
                if (placemarkName.toUpperCase().contains(searchText.toUpperCase()))
                {
                    strings[0] = Integer.toString(i);
                    strings[1] = placemark.getName() + ", " + placemark.getDescription();
                    cursor.addRow(strings);
                }
            }
        }
        String[] from = {"text"};
        int[] to = {R.id.listentry};
        return new SimpleCursorAdapter(context, R.layout.suggestions_layout, cursor, from, to);
    }
}
