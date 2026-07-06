package com.example.app_translate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_translate.BuildConfig
import com.example.app_translate.ui.theme.LightPurpleColor
import com.example.app_translate.ui.theme.PurpleColor
import com.example.app_translate.ui.theme.WhiteColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueScreen() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val apiKey = BuildConfig.GEMINI_API_KEY

    suspend fun sendToGemini(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", userMessage)
                                })
                            })
                        })
                    })
                }

                conn.outputStream.write(body.toString().toByteArray())

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("AI Chat", fontWeight = FontWeight.Bold, color = PurpleColor)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = WhiteColor)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WhiteColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleColor,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val userMsg = inputText.trim()
                            messages.add(ChatMessage(userMsg, isUser = true))
                            inputText = ""
                            isLoading = true
                            scope.launch {
                                val reply = sendToGemini(userMsg)
                                messages.add(ChatMessage(reply, isUser = false))
                                isLoading = false
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    containerColor = PurpleColor,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = WhiteColor)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🤖", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Ask the AI anything",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            items(messages) { msg ->
                ChatBubble(msg)
            }

            if (isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                            color = WhiteColor,
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                "...",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                fontSize = 20.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = if (message.isUser)
                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            else
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = if (message.isUser) PurpleColor else WhiteColor,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = if (message.isUser) WhiteColor else Color.Black,
                fontSize = 15.sp
            )
        }
    }
}