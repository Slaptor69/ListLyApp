package com.example.auth


import com.example.auth.exceptions.InvalidCredentialsException
import com.example.auth.exceptions.UserAlreadyExistsException
import com.example.security.JwtService
import com.example.security.PasswordHasher
import com.example.user.User
import com.example.user.UserRepository

class AuthService(private val userRepository: UserRepository) {

    fun login(login:String, password:String ): String {
        val user = userRepository.findByLogin(login)?: throw InvalidCredentialsException()

        if (!PasswordHasher.verify(password,user.passwordHash)){
            throw InvalidCredentialsException()
        }

        return JwtService.generateToken(user)
    }

    fun register(login:String, password:String){
        if (userRepository.findByLogin(login)!=null) throw UserAlreadyExistsException()

        val hash = PasswordHasher.hash(password)

        userRepository.save(User(login = login , passwordHash = hash))
    }
}