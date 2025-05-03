package uk.co.coroutines.enviropi.client

import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

interface ISensor : Closeable {
  val dataFlow: StateFlow<Data>
}
