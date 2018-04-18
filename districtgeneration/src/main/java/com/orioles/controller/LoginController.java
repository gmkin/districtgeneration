package com.orioles.controller;

import java.util.List;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.orioles.model.User;
import com.orioles.persistence.UserRepository;

@RestController
public class LoginController {
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private HttpSession httpSession;
  
  @PostMapping("/login")
  public User login(@RequestParam String email, @RequestParam String password){
    User user = (User) httpSession.getAttribute("user");
    System.out.println(user);
    if(user != null){
      return user;
    }

    List<User> users = userRepository.findByEmail(email);
    if(users.isEmpty()) {
	  System.out.println("Invalid email");
      return null;
    }

    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    user = users.get(0);
    if(encoder.matches(password, user.getPassword())){
      httpSession.setAttribute("user", user);
      return user;
    }
    System.out.println("Invalid credentials");
    return null;
  }
  
  @RequestMapping("/logout")
  public String logout(){
    httpSession.invalidate();
    return "OK";
  }
}
