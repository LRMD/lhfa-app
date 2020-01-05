package com.lt.lrmd.lhfa;

import android.os.AsyncTask;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class KMZDataHelper {

    private MainActivity delegateActivity;
    private String kmzFileName = "lhfa-updated.kmz";
    private ArrayList<Placemark> mPlaceMarks = null;

    public KMZDataHelper(MainActivity activity) {
        delegateActivity = activity;
    }

    public ArrayList<Placemark> getAllPlacemarks() {
        mPlaceMarks = new ArrayList<>();
        new ParseKMZ().execute(kmzFileName);
        return mPlaceMarks;
    }

    private class ParseKMZ extends AsyncTask<String, Void, Boolean>
    {
        @Override
        protected Boolean doInBackground(String... strings) {
            InputStream inputStream = null;
            ByteArrayOutputStream bOutput = new ByteArrayOutputStream();
            try {
                inputStream = delegateActivity.getAssets().open(strings[0]);
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry ze = null;
            try {
                ze = zis.getNextEntry();
                while(ze != null){
                    String fileName = ze.getName();
                    if (fileName.equals("doc.kml"))
                    {
                        int len;
                        byte[] buffer = new byte[1024];
                        while ((len = zis.read(buffer)) > 0) {
                            bOutput.write(buffer, 0, len);
                        }
                        zis.closeEntry();
                        break;
                    }
                    zis.closeEntry();
                    ze = zis.getNextEntry();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            String bString = bOutput.toString();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = null ;
            try {
                builder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            try {
                Document document = builder.parse(new ByteArrayInputStream(bOutput.toByteArray()));
                NodeList folderElements = document.getElementsByTagName("Folder");
                for (int i = 0; i < folderElements.getLength(); i++) {
                    Node folderNode = folderElements.item(i);
                    if (folderNode.hasChildNodes()) {
                        int j = 0;
                        for (j = 0; j < folderNode.getChildNodes().getLength(); j++) {
                            Node placeMarkNode = folderNode.getChildNodes().item(j);
                            if (placeMarkNode.getNodeName().equals("Placemark")) {
                                NodeList placemarkNodeList = placeMarkNode.getChildNodes();
                                Placemark placemark = new Placemark();
                                for (int m = 0; m < placemarkNodeList.getLength(); m++)
                                {
                                    Node item = placemarkNodeList.item(m);
                                    if (item.getNodeName().equals("name"))
                                    {
                                        placemark.setName(item.getTextContent());
                                    }
                                    if (item.getNodeName().equals("description"))
                                    {
                                        placemark.setDescription(item.getTextContent());
                                    }
                                    if (item.getNodeName().equals("Point"))
                                    {
                                        NodeList pointNodes = item.getChildNodes();
                                        for (int k = 0; k < pointNodes.getLength(); k++)
                                        {
                                            Node pointNode = pointNodes.item(k);
                                            if (pointNode.getNodeName().equals("coordinates"))
                                            {
                                                String coordinates = pointNode.getTextContent();
                                                String []values = coordinates.split(",");
                                                double longitude = Double.parseDouble(values[0]);
                                                placemark.setLongitude(longitude);
                                                double latitude = Double.parseDouble(values[1]);
                                                placemark.setLatitude(latitude);
                                            }
                                        }
                                    }
                                }
                                if (placemark.getDescription() != null)
                                    mPlaceMarks.add(placemark);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            delegateActivity.processFinished();
        }
    }

}
