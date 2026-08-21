# Lumina 📽️

**Lumina** es un reproductor multimedia minimalista de alto rendimiento diseñado para la simplicidad y la elegancia. Construido con **Compose Multiplatform**, ofrece una estética monocromática distintiva y herramientas avanzadas para la proyección de textos bíblicos, ideal para entornos litúrgicos y presentaciones.

## ✨ Características Principales

- **🎬 Reproductor Multimedia Potente**: Basado en `vlcj`, compatible con casi todos los formatos de audio y video.
- **📖 Proyección Bíblica Integrada**: Lógica avanzada para buscar, seleccionar y proyectar versículos con un solo clic.
- **🎨 Estética Monocromática**: Interfaz limpia, sin distracciones, utilizando bordes rectos y una paleta de colores minimalista.
- **⚙️ Personalización Total**:
    - Ajuste dinámico de tamaño, color y familia de fuente para proyecciones.
    - Buscador interactivo de tipografías del sistema.
    - Soporte para carga de archivos de fuentes externos (.ttf, .otf).
- **⌨️ Atajos de Teclado**: Sistema de shortcuts totalmente configurables para operar el reproductor sin ratón.
- **🌙 Modo Oscuro Nativo**: Diseñado para integrarse perfectamente en entornos de baja luminosidad.

## 🚀 Instalación y Uso

### Ejecución en Desarrollo

Lumina está estructurado como un proyecto Kotlin Multiplatform (KMP).

- **Ejecución Estándar**:
  ```bash
  ./gradlew :desktopApp:run
  ```
- **Hot Reload** (Desarrollo rápido):
  ```bash
  ./gradlew :desktopApp:hotRun --auto
  ```

### Compilación y Distribución

El proyecto está configurado para generar instaladores nativos en múltiples plataformas:

- **Windows**: `.exe` y `.msi` (`./gradlew :desktopApp:packageMsi`)
- **Linux**: `.deb`, `.AppImage` y `.tar.gz` (`./gradlew :desktopApp:packageDeb`)
- **macOS**: `.dmg` (`./gradlew :desktopApp:packageDmg`)

> [!WARNING]
> Es necesario tener instalado VLC

## 🛠️ Tecnologías

- **Kotlin** & **Compose Multiplatform** para la UI.
- **VLCJ** como motor de reproducción de medios.
- **Kotlinx Serialization** para persistencia de ajustes.
- **GitHub Actions** para CI/CD automático.

---

Desarrollado con ❤️ por [GeracioDev](https://github.com/geraciodev).
