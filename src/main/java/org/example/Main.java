package org.example;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Use correct typing Main path/to/jar...");
            return;
        }

        String mainClassName = args[0];
        String[] jarPaths = new String[args.length - 1];
        System.arraycopy(args, 1, jarPaths, 0, jarPaths.length);

        try {
            boolean allDependenciesPresent = checkDependencies(mainClassName, jarPaths);
            if (allDependenciesPresent) {
                System.out.println("All dependencies are present");
            } else {
                System.out.println("Some dependencies are missing");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkDependencies(String mainClassName, String[] jarPaths) throws IOException, ClassNotFoundException {
        Set<String> availableDependencies = new HashSet<>();
        for (String jarPath : jarPaths) {
            File file = new File(jarPath);
            if (file.exists() && file.isFile()) {
                try (JarFile jarFile = new JarFile(file)) {
                    jarFile.stream()
                            .filter(entry -> entry.getName().endsWith(".class"))
                            .forEach(entry -> availableDependencies.add(
                                    entry.getName().replace('/', '.').replace(".class", ""))
                            );
                }
            } else {
                System.out.println("Invalid JAR path: " + jarPath);
            }
        }

        URL[] urls = new URL[jarPaths.length];
        for (int i = 0; i < jarPaths.length; i++) {
            urls[i] = new File(jarPaths[i]).toURI().toURL();
        }

        try (URLClassLoader classLoader = new URLClassLoader(urls)) {
            Class<?> mainClass = classLoader.loadClass(mainClassName);
            Set<String> requiredDependencies = new HashSet<>();
            collectDependencies(mainClass, requiredDependencies);

            Set<String> additionalDependencies = new HashSet<>(availableDependencies);
            for (String className : additionalDependencies) {
                collectDependencies(classLoader.loadClass(className), availableDependencies);
            }

            requiredDependencies.removeAll(availableDependencies);
            if (!requiredDependencies.isEmpty()) {
                System.out.println("Missing dependencies: " + requiredDependencies);
                return false;
            }
        }
        return true;
    }

    private static void collectDependencies(Class<?> clas, Set<String> dependencies) {
        dependencies.add(clas.getName());

        if (clas.getSuperclass() != null && !dependencies.contains(clas.getSuperclass().getName())) {
            collectDependencies(clas.getSuperclass(), dependencies);
        }
        for (Class<?> interfaceClass : clas.getInterfaces()) {
            if(!dependencies.contains(interfaceClass.getName())) {
                collectDependencies(interfaceClass, dependencies);
            }
        }
        for (var field : clas.getDeclaredFields()) {
            dependencies.add(field.getType().getName());
        }
        for (var method : clas.getDeclaredMethods()) {
            dependencies.add(method.getReturnType().getName());
            for (Class<?> parameterType : method.getParameterTypes()) {
                dependencies.add(parameterType.getName());
            }
            for (Class<?> exceptionType : method.getExceptionTypes()) {
                dependencies.add(exceptionType.getName());
            }
        }
    }
}



