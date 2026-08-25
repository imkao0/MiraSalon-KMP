package iz.mkao.mirasalon.core.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class SalonNavigator(
    override val backStacks: Map<NavKey, NavBackStack<NavKey>>,
    private val _currentTab: MutableState<NavKey>,
    override val startRoute: NavKey
) : Navigator {

    override val currentTab: State<NavKey> = _currentTab

    override fun navigateTo(key: NavKey): Boolean {
        if (backStacks.containsKey(key)) {
            _currentTab.value = key
            return true
        } else {
            val stack = backStacks[_currentTab.value] ?: return false
            // Prevent duplicate pushes of the same key on top of the stack
            if (stack.lastOrNull() == key) return false
            stack.add(key)
            return true
        }
    }

    override fun replace(key: NavKey): Boolean {
        if (backStacks.containsKey(key)) {
            _currentTab.value = key
            return true
        } else {
            val stack = backStacks[_currentTab.value] ?: return false
            if (stack.isNotEmpty()) {
                stack.removeAt(stack.size - 1)
            }
            stack.add(key)
            return true
        }
    }

    override fun clearAndNavigate(key: NavKey): Boolean {
        if (backStacks.containsKey(key)) {
            _currentTab.value = key
            val stack = backStacks[key] ?: return false
            while (stack.size > 1) {
                stack.removeAt(stack.size - 1)
            }
            return true
        } else {
            val stack = backStacks[_currentTab.value] ?: return false
            stack.clear()
            stack.add(key)
            return true
        }
    }

    override fun pop(): Boolean {
        val currentStack = backStacks[_currentTab.value] ?: return false
        return when {
            currentStack.size > 1 -> {
                currentStack.removeAt(currentStack.size - 1)
                true
            }
            _currentTab.value != startRoute -> {
                _currentTab.value = startRoute
                true
            }
            else -> false
        }
    }

    override fun popUntil(predicate: (NavKey) -> Boolean) {
        val currentStack = backStacks[_currentTab.value] ?: return
        while (currentStack.size > 1) {
            val last = currentStack.lastOrNull() ?: break
            if (predicate(last)) break
            currentStack.removeAt(currentStack.size - 1)
        }
        
        val last = currentStack.lastOrNull()
        if (last == null || (!predicate(last) && _currentTab.value != startRoute)) {
            _currentTab.value = startRoute
            val startStack = backStacks[startRoute] ?: return
            while (startStack.size > 1) {
                val startLast = startStack.lastOrNull() ?: break
                if (predicate(startLast)) break
                startStack.removeAt(startStack.size - 1)
            }
        }
    }
}
