package com.example.tasklistapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tasklistapp.ui.theme.TaskListAppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// Esta parte se encagra de ejecutar el programa
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskListAppTheme {
                ToDoListScreen()
            }
        }
    }
}

//Componente principal
@Composable
fun ToDoListScreen() {
    val context = LocalContext.current

    // State Hoisting: Elevamos el estado para gestionar las tareas y las imágenes
    val tasks = remember { mutableStateListOf<Task>() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Solicitar permisos en el inicio
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    //Manda a llamar a ToDoListContent con las funciones de delete y remove agregadas
    ToDoListContent(
        tasks = tasks,
        onAddTask = { title, imageUri ->
            tasks.add(Task(title, imageUri))
        },
        onRemove = {task -> tasks.remove(task)},
        onEdit = {task, title, imageUri -> tasks[tasks.indexOf(task)] = Task(title, imageUri)}
    )
}

//Contiene el form para crear una nueva tarea y la lista de tareas
@Composable
fun ToDoListContent(
    tasks: List<Task>,
    onAddTask: (String, String?) -> Unit,
    onRemove: (Task) -> Unit,
    onEdit: (Task, String, String?) -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "To Do List", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        //Estos son los campos de texto y botones para guardar una tarea con o sin imagen
        OutlinedTextField(
            value = newTaskTitle,
            onValueChange = { newTaskTitle = it },
            label = { Text("Task Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Pick Image")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                //Solo se manda si el texto no esta vacio
                if (newTaskTitle.isNotEmpty()) {
                    onAddTask(newTaskTitle, selectedImageUri)
                    newTaskTitle = ""
                    selectedImageUri = null
                }
            }
        ) {
            Text("Add Task")
        }

        Spacer(modifier = Modifier.height(16.dp))

        //Manda a llamar a la lista de tareas
        ToDoList(tasks, onRemove = onRemove, onEdit = onEdit)
    }
}

//Lista de tareas
@Composable
fun ToDoList(tasks: List<Task>, onRemove: (Task) -> Unit, onEdit: (Task, String, String?) -> Unit) {

    //Utiliza LazyColumn para enumerar las tareas una por una
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks) { task ->
            TaskItem(task, onRemove = { onRemove(task) }, onEdit = onEdit)
        }
    }
}

//Representa cada tarea individualmente
@Composable
fun TaskItem(task: Task, onRemove: () -> Unit,
             onEdit: (Task, String, String?) -> Unit) {
    var editar by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf(task.title) }
    var newImage by remember { mutableStateOf(task.imageUri) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        newImage = uri?.toString()
    }

    //Card donde se muestra el contenido
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                task.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
            }
            Row(
                modifier = Modifier
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                //Botones para mandar a editar o eliminar la tarea
                IconButton(onClick = { editar = true }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Task")
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task"
                    )
                }
            }
        }
    }

    //Si editar esta como true, va a desplegar un AlertDialog, donde el usuario puede ingresar la nueva info
    if (editar) {
        AlertDialog(
            onDismissRequest = { editar = false },
            title = { Text("Edit Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("New Title") }
                    )
                    Spacer(modifier = Modifier.height(8.dp).fillMaxWidth())
                    Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Pick a new image")
                    }
                }
            },
            //Manda a editar la info, Solo se manda si el texto no esta vacio
            confirmButton = {
                Button(onClick = {
                    if (newTitle.isNotBlank()){
                        onEdit(task, newTitle, newImage)
                        editar = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { editar = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

//Esta parte solo sirve para el preview
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TaskListAppTheme {
        ToDoListScreen()
    }
}

//Los objetos tipo Task
data class Task(val title: String, val imageUri: String?)