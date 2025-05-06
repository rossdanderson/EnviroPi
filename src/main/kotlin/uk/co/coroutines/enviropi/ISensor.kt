package uk.co.coroutines.enviropi

import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

interface ISensor : Closeable {
  val dataFlow: StateFlow<Data>
}
