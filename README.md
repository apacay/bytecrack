# ByteCrack

Juego de descifrado de códigos con estética hacker para Android.

## Requisitos

- **JDK 17** (Android Studio lo incluye)
- **Gradle 9.3.1** (incluido vía wrapper)
- Android Studio Ladybug o superior (recomendado)

## Cómo ejecutar

1. Abre el proyecto en **Android Studio**
2. Android Studio usará su JDK embebido (17) automáticamente
3. Sincroniza Gradle (File → Sync Project with Gradle Files)
4. Conecta un dispositivo Android o inicia un emulador
5. Ejecuta la app (Run → Run 'app')

## Desde línea de comandos

Si tienes JDK 17 configurado en `JAVA_HOME`:

```powershell
.\gradlew.bat assembleDebug
```

Para instalar en dispositivo conectado:

```powershell
.\gradlew.bat installDebug
```

## Estructura del proyecto

- `app/src/main/java/com/bytecrack/` - Código fuente
  - `domain/` - Lógica del juego (CodeGenerator, GuessEvaluator, TierCalculator)
  - `data/` - Room DB, entidades
  - `ui/` - Compose screens, ViewModel, tema
  - `di/` - Hilt dependency injection
