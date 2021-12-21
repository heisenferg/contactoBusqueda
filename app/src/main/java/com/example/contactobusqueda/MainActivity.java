package com.example.contactobusqueda;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemLongClickListener {
    EditText edNombre;
    private final int PETICION_PERMISOS=1;
    private final String TAG="RiberaDelTajo";
    private boolean tengo_permisos =false;
    private ListView lstContactos;
    private ImageView imgContacto;
    private TextView txtMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //solicitud de permisos
        if(checkSelfPermission("android.permission.READ_CONTACTS")
                != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission("android.permission.SEND_SMS")
                        != PackageManager.PERMISSION_GRANTED )
            requestPermissions(new String[]{
                            "android.permission.READ_CONTACTS",
                            "android.permission.SEND_SMS"},
                    PETICION_PERMISOS);
        else
            tengo_permisos =true;

        edNombre = findViewById(R.id.edNombre);
        imgContacto = findViewById(R.id.imageViewPerfil);
        txtMensaje = findViewById(R.id.edMensaje);
        lstContactos = findViewById(R.id.lstContactos);
        lstContactos.setOnItemLongClickListener(this);
        edNombre.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (tengo_permisos){
                    ArrayList<String> lista_contactos = buscar(edNombre.getText().toString());

                    lstContactos=findViewById(R.id.lstContactos);
                    lstContactos.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                            R.layout.listview, lista_contactos));
                }
                return true;
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PETICION_PERMISOS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tengo_permisos=true;
            } else
                tengo_permisos=false;
        }
    }

    @SuppressLint("Range")
    public ArrayList<String> buscar (String contacto){
        String proyeccion[]={
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
            ContactsContract.Contacts.PHOTO_ID};
        String filtro = ContactsContract.Contacts.DISPLAY_NAME + " like ?";
        String args_filtro[]={"%" + contacto + "%"};
        ArrayList<String> lista_contactos = new ArrayList<String>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, proyeccion,
                filtro, args_filtro, null);

        if (cur.getCount() >0){
            while (cur.moveToNext()){
                // Obtener ID de contacto
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                // Obtener nombre
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                //Si tiene teléfono lo agregamos a la lista de contactos
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)))>0) {
                    lista_contactos.add(name);
                }

            }

}
        cur.close();
        return lista_contactos;
    }

    @SuppressLint("Range")
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        TextView t = (TextView) view;
        String nombreContacto = t.getFontFeatureSettings().toString();
        String proyeccion [] = {ContactsContract.Contacts._ID};
        String filtro = ContactsContract.Contacts.DISPLAY_NAME + " =";
        String args_filtro[] = {nombreContacto};

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, proyeccion, filtro, args_filtro, null);

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                int identificador = cur.getInt(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));

                // Establecer foto de contacto
                imgContacto.setImageBitmap(BitmapFactory.decodeStream(abrirFoto(identificador)));
                enviarSMS(identificador, "Hola, quedamos?");
            }
        }
        cur.close();
        return true;
    }

    public InputStream abrirFoto (int identificador) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, identificador);
        InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(),
                contactUri, true);
        return inputStream;
    }

    @SuppressLint("Range")
    private void enviarSMS(int identificador, String mensaje) {
        ContentResolver cr = getContentResolver();
        SmsManager smsManager = SmsManager.getDefault();
        Cursor cursorTelefono = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String []{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String []{String.valueOf(identificador)}, null);

        while (cursorTelefono.moveToNext()) {
             String telefono = cursorTelefono.getString(
                    cursorTelefono.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));

             try {
                 smsManager.sendTextMessage(telefono, null, mensaje, null, null);
                 Log.d(TAG, "SMS enviado.");
                 txtMensaje.setText("Mensaje [" + mensaje + "] enviado a:");

        } catch (Exception e) {
                 Log.d(TAG, "No se pudo enviar el SMS");
                 txtMensaje.setText("Mensaje no enviado a:");
                 e.printStackTrace();
             }
    }
        cursorTelefono.close();



    }
}