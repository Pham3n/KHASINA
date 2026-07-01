package play.zulu.khasina

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GameInfo(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int
)

@Composable
fun DashboardScreen(
    viewModel: GameViewModel,
    onLaunchGame: (String) -> Unit
) {
    val games = listOf(
        GameInfo("khasina", "KHASINA", "Strategic card capturing game", R.drawable.bgbr),
        GameInfo("umlabalaba", "UMLABALABA", "Traditional board game", R.drawable.crd) // Placeholder icon
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bgbr),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLAYZULU",
                    color = Color(0xFFE0BC7A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                IconButton(onClick = { viewModel.isProfileVisible = true }) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFFE0BC7A), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Select a Game",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(games) { game ->
                    GameCard(game) { onLaunchGame(game.id) }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            ChatBox(viewModel, modifier = Modifier.height(200.dp))
        }

        if (viewModel.isProfileVisible) {
            ProfileDropdownMenu(
                viewModel = viewModel,
                onClose = { viewModel.isProfileVisible = false }
            )
        }
        
        if (viewModel.isFriendsVisible) {
            FriendsDropdownMenu(
                viewModel = viewModel,
                onClose = { viewModel.isFriendsVisible = false }
            )
        }
        
        if (viewModel.isUserDetailVisible) {
            UserDetailDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.isUserDetailVisible = false }
            )
        }
    }
}

@Composable
fun GameCard(game: GameInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B12).copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(80.dp).background(Color(0xFF5A3822), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                 // In a real app, use a real icon. Here we just show the title initials
                 Text(game.title.take(1), color = Color(0xFFEBC98F), fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = game.title,
                color = Color(0xFFEBC98F),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = game.description,
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
