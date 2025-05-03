package uk.co.coroutines.enviropi.client

import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

interface ISensor : Closeable {
  val dataFlow: StateFlow<Data>
}
