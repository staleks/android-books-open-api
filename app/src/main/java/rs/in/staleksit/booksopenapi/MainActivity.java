package rs.in.staleksit.booksopenapi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import rs.in.staleksit.booksopenapi.adapter.BookAdapter;
import rs.in.staleksit.booksopenapi.adapter.BookArrayAdapter;
import rs.in.staleksit.booksopenapi.model.BookItem;
import rs.in.staleksit.booksopenapi.model.BookSearch;
import rs.in.staleksit.booksopenapi.toolbox.GsonRequest;


public class MainActivity extends Activity {

    private static final String TAG_NAME = "Books-Open-API";

    private EditText etQuery;
    private Button btnSearch;

    private ListView lvQueryResult;

    // private BookAdapter adapter;

    private List<BookItem> bookItemList = new ArrayList<BookItem>(0);
    private BookArrayAdapter mAdapter;

    private ProgressDialog pDialog;

    private LinearLayout llMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        etQuery = (EditText) findViewById(R.id.etQuery);
        if (null != etQuery) {
            etQuery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    etQuery.setText("");
                }
            });
        }

        btnSearch = (Button) findViewById(R.id.btnSearch);
        // check if btnSearch is located in view
        if (null != btnSearch) {
            btnSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pDialog = new ProgressDialog(MainActivity.this);
                    pDialog.setMessage("Downloading ...");
                    pDialog.show();

                    mAdapter.clear();

                    RequestQueue queue = BookAppVolley.getRequestQueue();
                    GsonRequest<BookSearch> searchRequest = new GsonRequest<BookSearch>(
                            Request.Method.GET,
                            BookItemContract.OPEN_IT_BOOKS_API_ENDPOINT_SEARCH + etQuery.getText().toString(),
                            BookSearch.class,
                            myRequestSuccessListener(),
                            myRequestErrorListener());
                    queue.add(searchRequest);
                }
            });
        }

        lvQueryResult = (ListView) findViewById(R.id.lvQueryResult);

        mAdapter = new BookArrayAdapter(this, 0, bookItemList, BookAppVolley.getImageLoader());
        lvQueryResult.setAdapter(mAdapter);
        lvQueryResult.setOnScrollListener(new EndlessScrollListener());

        lvQueryResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BookItem selectedBookItem = (BookItem) parent.getAdapter().getItem(position);
                Intent bookItemIntent = new Intent(MainActivity.this, BookItemActivity.class);
                bookItemIntent.putExtra("rs.in.staleksit.booksopenapi.BOOK_ID", selectedBookItem.getId());
                startActivity(bookItemIntent);
            }
        });

        llMainActivity = (LinearLayout) findViewById(R.id.llMainActivity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        String appVersionName = "1.0.0";
        String aboutApplicationTitle = "BooksOpenApi - v";
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            appVersionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException nnfEx) {
            appVersionName = "1.0.0";
        }
        String completeTitle = aboutApplicationTitle + appVersionName;


        //noinspection SimplifiableIfStatement
        if (id == R.id.about) {
            new AlertDialog.Builder(this)
                    .setTitle(completeTitle)
                    .setMessage(R.string.dlgAboutMessage)
                    .setPositiveButton(R.string.dlgAboutBtnClose,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    dialog.cancel();
                                }
                            }).show();

            return true;        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * in case of success of volley request
     * @return
     */
    private Response.Listener<BookSearch> myRequestSuccessListener() {
        Response.Listener<BookSearch> response = new Response.Listener<BookSearch>() {
            @Override
            public void onResponse(BookSearch response) {

                if (response.getBooks() != null) {
                    for (BookItem item : response.getBooks()) {
                        bookItemList.add(item);
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }
        };
        hideProgressDialog();
        return response;
    }

    private Response.ErrorListener myRequestErrorListener() {
        Response.ErrorListener errorResponse = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.custom_dialog);
                dialog.setTitle("Error");

                TextView textView = (TextView) dialog.findViewById(R.id.cdTextView);
                textView.setText(volleyError.toString());

                Button cdButton = (Button) dialog.findViewById(R.id.cdButton);
                cdButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        };
        hideProgressDialog();
        return errorResponse;
    }

    private void hideProgressDialog() {
        if (null != pDialog) {
            pDialog.dismiss();
            pDialog = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Tracker t = BookAppVolley.getTracker(BookAppVolley.TrackerName.APP_TRACKER);
        t.setScreenName("OpenBooksAPI-screenView");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    private void loadPage(int currentPage) {
        RequestQueue queue = BookAppVolley.getRequestQueue();

        int startIndex = currentPage + bookItemList.size();
        GsonRequest<BookSearch> myReq = new GsonRequest<BookSearch>(Request.Method.GET,
                BookItemContract.OPEN_IT_BOOKS_API_ENDPOINT_SEARCH + etQuery.getText().toString() + "/page/" + startIndex,
                BookSearch.class,
                myRequestSuccessListener(),
                myRequestErrorListener());

        queue.add(myReq);
    }


    /**
     * Detects when user is close to the end of the current page and starts loading the next page
     * so the user will not have to wait (that much) for the next entries.
     *
     * @author Ognyan Bankov
     */
    public class EndlessScrollListener implements OnScrollListener {
        // how many entries earlier to start loading next page
        private int visibleThreshold = 5;
        private int currentPage = 0;
        private int previousTotal = 0;
        private boolean loading = true;

        public EndlessScrollListener() {

        }

        public EndlessScrollListener(int visibleThreshold) {
            this.visibleThreshold = visibleThreshold;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                             int totalItemCount) {
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                    currentPage++;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                loadPage(currentPage);
                loading = true;
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        public int getCurrentPage() {
            return currentPage;
        }
    }


}
