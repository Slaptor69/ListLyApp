package auth

import com.example.auth.AuthService
import com.example.auth.exceptions.UserAlreadyExistsException
import com.example.user.User
import com.example.user.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith


class AuthServiceTest {
    private val repository = mockk<UserRepository>()
    private val service = AuthService(repository)

    @Test
    fun `register throws UserAlreadyExistsException if login already exists`()
    {
        val login = "login1"
        val user = User(login=login, passwordHash = "239344")

        every{repository.findByLogin(login)} returns user

        assertFailsWith<UserAlreadyExistsException> {
            service.register(user.login,user.passwordHash)
        }

        verify(exactly = 0) { repository.save(any()) }

    }


    @Test
    fun `successful registration`(){
        val login = "login1"
        val user = User(login = login, passwordHash = "234445")

        every { repository.findByLogin(login)} returns null

        every { repository.save(any()) } just Runs

        service.register(login,user.passwordHash)

        verify(exactly = 1) { repository.save(any()) }

    }
}