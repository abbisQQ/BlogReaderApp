package com.abbisqq.blogreaderapp;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity implements ResultsCallback{

    PlaceholderFragment taskFragment;
    ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        list = (ListView)findViewById(R.id.list_view);
        if(savedInstanceState==null){
            taskFragment = new PlaceholderFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(taskFragment,"MyFragment")
                    .commit();
        }else {
            taskFragment = (PlaceholderFragment)getSupportFragmentManager().findFragmentByTag("MyFragment");
        }
        taskFragment.startTask();
    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onPostExecute(ArrayList<HashMap<String, String>> results) {
        list.setAdapter(new MyAdapter(this,results));
    }


    public static class PlaceholderFragment extends Fragment{

        MainTask downloadTask;
        ResultsCallback callback;
        public PlaceholderFragment() {
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            callback = (ResultsCallback)activity;

            if(downloadTask!=null){
                downloadTask.onAttach(callback);
            }


        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            callback=null;

            if(downloadTask!=null){
                downloadTask.onDetach();
            }


        }

        public void startTask(){
            if(downloadTask!=null){
                //if there is a task cancel it even if you must interrupt it
                downloadTask.cancel(true);
            }else {
                downloadTask = new MainTask(callback);
                downloadTask.execute();
            }
        }

    }



    public static class MainTask extends AsyncTask<Void, Void, ArrayList<HashMap<String, String>>>{

        ResultsCallback callback=null;

        public MainTask(ResultsCallback callback) {
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {

            if(callback!=null){
                callback.onPreExecute();
            }

        }

        public void onAttach(ResultsCallback callback){
            this.callback = callback;

        }

        public void onDetach(){
            callback=null;

        }

        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(Void... params) {

            String downloadURL = "http://feeds.feedburner.com/techcrunch/android?format=xml";
            ArrayList<HashMap<String, String>> results =  new ArrayList<>();
            try {
                URL url = new URL(downloadURL);
                HttpURLConnection connection = (HttpURLConnection)url
                        .openConnection();
                InputStream inputStream = connection.getInputStream();
                results = processXMl(inputStream);
                processXMl(inputStream);

                connection.setRequestMethod("GET");
            } catch (MalformedURLException e) {

                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }


            return results;
        }

        @Override
        protected void onPostExecute(ArrayList<HashMap<String, String>> result) {

           if(callback!=null){
               callback.onPostExecute(result);
           }
        }
        public ArrayList<HashMap<String, String>> processXMl(InputStream inputStream) throws Exception{
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder builder = documentBuilderFactory
                    .newDocumentBuilder();
            Document xmlDocument = builder.parse(inputStream);

            Element rootElement = xmlDocument.getDocumentElement();

            Log.v("FUN",rootElement.getTagName());

            NodeList itemList = rootElement.getElementsByTagName("item");
            NodeList itemChildren;
            int count =0;
            ArrayList<HashMap<String, String >> results = new ArrayList<>();
            HashMap<String, String> currentMap = null;

            Node currentItem;
            Node currentChild;
            for(int i=0;i<itemList.getLength();i++){
                currentItem = itemList.item(i);


                currentMap = new HashMap<>();


                itemChildren  =   currentItem.getChildNodes();
                for(int j=0;j<itemChildren.getLength();j++){
                    currentChild = itemChildren.item(j);
                    if(currentChild.getNodeName().equalsIgnoreCase("title")){
                        currentMap.put("title",currentChild.getTextContent());

                    }
                    if(currentChild.getNodeName().equalsIgnoreCase("pubDate")){

                        currentMap.put("pubDate",currentChild.getTextContent());
                    }
                    if(currentChild.getNodeName().equalsIgnoreCase("description")){
                        currentMap.put("description",currentChild.getTextContent());

                    }
                    if(currentChild.getNodeName().equalsIgnoreCase("media:thumbnail")){

                        count++;
                        if(count==2){
                            currentMap.put("imageURL",currentChild.getAttributes()
                                    .item(0).getTextContent());

                        }
                    }
                    }
                    if(currentMap!=null&&!currentMap.isEmpty()){
                        results.add(currentMap);
                    }
                    count=0;
                }

                return results;
            }

        }

    }

    interface ResultsCallback{
        public void onPreExecute();
        public void onPostExecute(ArrayList<HashMap<String,String>> results);
    }
class MyAdapter extends BaseAdapter{


    ArrayList<HashMap<String,String>> dataSource= new ArrayList<>();
    Context context;
    LayoutInflater inflater;

    public MyAdapter(Context context,ArrayList<HashMap<String, String>> dataSource) {
        this.dataSource = dataSource;
        this.context =context;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return dataSource.size();
    }

    @Override
    public Object getItem(int position) {
        return dataSource.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row =convertView;
        MyHolder holder = null;
        if(row==null){
            row = inflater.inflate(R.layout.custom_row,parent,false);
            holder = new MyHolder(row);
            row.setTag(holder);
        }else {
           holder =  (MyHolder)row.getTag();
        }

        HashMap<String,String> currentItem = dataSource.get(position);
        holder.title.setText(currentItem.get("title"));
        holder.publishedDate.setText(currentItem.get("pubDate"));
        Glide.with(parent.getContext()).load(currentItem.get("imageURL")).into(holder.imageView);
        //holder.imageView.setImageURI(Uri.parse(String.valueOf(currentItem.get("imageURL"))));
        //holder.description.setText(currentItem.get("description"));
        return row;
    }
}
class MyHolder {
    TextView title,publishedDate,description;
    ImageView imageView;

    public MyHolder(View view) {
        title = (TextView)view.findViewById(R.id.title_view);
        description = (TextView)view.findViewById(R.id.description_view);
        publishedDate = (TextView)view.findViewById(R.id.article_view);
        imageView = (ImageView)view.findViewById(R.id.list_image);
    }


}