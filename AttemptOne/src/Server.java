import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Server {
    private final Object userlock;
    private ServerSocket server;
    private static final String USERS_FILENAME = "./users.bin";

    public Server() {
        initAdmins();
        userlock = new Object();
    }

    /*
    Creates the users file the first time the server is ran.
     */
    public void initAdmins() {
        if (new File(USERS_FILENAME).exists())
            return;
        List<User> users = new ArrayList<>();
        users.add(new Admin("admin", "12345"));
        saveUsers(users);
    }

    public void saveUsers(List<User> users) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILENAME))) {
            oos.writeObject(users);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<User> loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USERS_FILENAME))) {
            return (List<User>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    void start() {
        try {
            server = new ServerSocket(8080);
            System.out.println("Server listening...");
            while (true) {
                Socket client = server.accept();

                Thread clientThread = new Thread(() -> {
                    System.out.println("Client Accepted.");
                    Scanner sc = null;
                    PrintStream out = null;
                    try {
                        sc = new Scanner(client.getInputStream());
                        out = new PrintStream(client.getOutputStream());
                        userMenu(sc, out);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (sc != null) {
                            sc.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    }
                });
                clientThread.start();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void userMenu(Scanner sc, PrintStream out) {
        while (true) {
            out.println("Login Y/N");
            if (!sc.nextLine().equalsIgnoreCase("Y")) {
                out.println("Goodbye!");
                return;
            }
            out.println("Please enter username:");
            String username = sc.nextLine();
            out.println("Please enter password:");
            String password = sc.nextLine();
            User user = login(username, password);
            if (user == null) {
                out.println("Error: Invalid login.");
                continue;
            }
            switch (user.getUserType()) {
                case ADMIN: {
                    adminMenu(sc, out, (Admin) user);
                    break;
                }
                case STUDENT: {
                    studentMenu(sc, out, (Student) user);
                }
                case TEACHER: {
                    teacherMenu(sc, out, (Teacher) user);
                }
            }
        }
    }

    private void studentMenu(Scanner sc, PrintStream out, Student user) {
        out.println("Logged in as Student");
        List<Grade> sortedGrades = user.getGrades()
                .stream()
                .sorted(Comparator.comparingInt(Grade::getSemester).thenComparing(Grade::getSubject))
                .collect(Collectors.toList());
        out.println(sortedGrades);
    }

    private void teacherMenu(Scanner sc, PrintStream out, Teacher teacher)
    {
        out.println("Logged in as teacher.");

        out.println("Enter student faculty number:");
        String facultyNumber = sc.nextLine();

        out.println("Enter subject:");
        String subject = sc.nextLine();

        out.println("Enter semester:");
        int semester = Integer.parseInt(sc.nextLine());

        out.println("Enter grade:");
        int gradeValue = Integer.parseInt(sc.nextLine());

        Grade grade = new Grade(subject, semester, gradeValue);

        synchronized (userlock)
        {
            List<User> users = loadUsers();
            for (User user : users)
            {
                if (user.getUsername().equals(facultyNumber) && user instanceof Student)
                {
                    Student student = (Student) user;

                    student.getGrades().add(grade);

                    saveUsers(users);

                    out.println("Success.");
                    return;
                }
            }

            out.println("No such student.");
        }
    }

    private void adminMenu(Scanner sc, PrintStream out, Admin user) {
        while (true) {
            out.println("Logged in as ADMIN");
            out.println("Please enter account type (STUDENT | TEACHER)");
            try {
                UserType userType = UserType.valueOf(sc.nextLine().toUpperCase());
                switch (userType) {
                    case TEACHER: {
                        out.println("Enter email:");
                        break;
                    }
                    case STUDENT: {
                        out.println("Enter faculty number:");
                        break;
                    }
                }
                String username = sc.nextLine();
                switch (userType) {
                    case TEACHER: {
                        out.println("Enter password:");
                        break;
                    }
                    case STUDENT: {
                        out.println("Enter EGN:");
                        break;
                    }
                }
                String password = sc.nextLine();
                User newUser = UserFactory.createUser(userType, username, password);
                synchronized (userlock) {
                    List<User> newUsers = loadUsers();
                    newUsers.add(newUser);
                    saveUsers(newUsers);
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (CredentialException e) {
                e.printStackTrace();
            }
        }
    }

    private User login(String username, String password) {
        synchronized (userlock) {
            for (User s : loadUsers()) {
                System.out.println(username.concat(s.getUsername()));
                if (username.equals(s.getUsername()) && password.equals(s.getPassword())) {
                    return s;
                }
            }
            return null;
        }
    }
}
