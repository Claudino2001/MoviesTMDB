package br.com.application.moviestmdb;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private ListView list;
    private static final String TAG = " MINHA TAG";
    List<Filme> filmes = new ArrayList<>();
    List<Genero> generos = new ArrayList<>();
    public BancoDeDados banco;
    public BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list = (ListView) findViewById(R.id.list);
        View rootView = findViewById(android.R.id.content);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.page_main);

        banco();

        consultaRetrofitGeneros();

        consultaRetrofitPopularMovies();

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.page_main:
                        return true;
                    case R.id.page_favoritos:
                        startActivity(new Intent(MainActivity.this, FavoritosActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        return true;
                    case R.id.page_search:
                        startActivity(new Intent(MainActivity.this, SearchActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        return true;
                }
                return false;
            }
        });

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Integer filme_id = filmes.get(i).getId();
                String filme_name = filmes.get(i).getOriginal_title();
                if(!banco.searchMovie(filme_id)){
                    inserirAosFavs(filme_id, filme_name);
                }
                return true;
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, DetalhesMovieActivity.class);
                intent.putExtra("filme_obj", (Serializable) filmes.get(i));
                intent.putExtra("generos_obj", (Serializable) generos);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

    }

    private void inserirAosFavs(int filme_id, String filme_nome) {
        AlertDialog.Builder msgBox = new AlertDialog.Builder(this);
        msgBox.setTitle("Adicionar filme aos favoritos.");
        msgBox.setIcon(R.drawable.ic_add);
        msgBox.setMessage("Tem certeza que deseja adicionar esse filme aos favoritos?");
        msgBox.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                banco.inserirDados(filme_id, filme_nome);
            }
        });
        msgBox.setNegativeButton("Não", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(MainActivity.this, "Operação cancelada.", Toast.LENGTH_SHORT).show();
            }
        });
        msgBox.show();
    }

    private void banco() {
        banco = new BancoDeDados(this);
    }

    private void consultaRetrofitGeneros() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Service.URL_BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service service = retrofit.create(Service.class);
        Call<Genres> request = service.GetAPIGeneros("pt-BR", "da0e4838c057baf77b75e5338ced2bb3");

        request.enqueue(new Callback<Genres>() {
            @Override
            public void onResponse(Call<Genres> call, Response<Genres> response) {
                if(!response.isSuccessful()){
                    Log.i(TAG, "Erro: " + response.code());
                }else{
                    Genres genres = response.body();

                    for(Genero genero: genres.getGenres()){
                        Log.i(TAG, String.format("GENERO: %s %s", genero.getId(), genero.getName()));
                    }
                    generos = genres.getGenres();
                }
            }

            @Override
            public void onFailure(Call<Genres> call, Throwable t) {
                Log.e(TAG, "Erro: " + t.getMessage());
            }
        });
    }


    private void mostrarFilmes() {
        Adapter_item_filme adapter = new Adapter_item_filme(this, filmes, generos);
        list.setAdapter(adapter);
    }


    private void consultaRetrofitPopularMovies() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Service.URL_BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service service = retrofit.create(Service.class);
        Call<GetPopularMovies> request =  service.GetAPIPopularMovies("pt-BR","da0e4838c057baf77b75e5338ced2bb3");

        request.enqueue(new Callback<GetPopularMovies>() {
            @Override
            public void onResponse(Call<GetPopularMovies> call, Response<GetPopularMovies> response) {
                if(!response.isSuccessful()){
                    Log.i(TAG, "Erro: " + response.code());
                }else{
                    //A requisição foi realizada com sucesso
                    GetPopularMovies getPopularMovies = response.body();

                    for(Filme f: getPopularMovies.getResults()){
                        Log.i("TAG",String.format("%s : %s",f.getTitle(), f.getOriginal_title()));
                    }
                    filmes = getPopularMovies.getResults();
                }
                mostrarFilmes();
            }

            @Override
            public void onFailure(Call<GetPopularMovies> call, Throwable t) {
                Log.e(TAG, "Erro: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.page_main);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}