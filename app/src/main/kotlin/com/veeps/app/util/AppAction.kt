package com.veeps.app.util

import com.veeps.app.feature.contentRail.model.Entities

interface AppAction {
	fun onAction(entity: Entities, tag: String) = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun focusDown() = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun focusUp() = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun focusLeft() = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun focusRight() = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun onEvent(entity: Entities, tag: String) = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun onEvent(entity: Entities) = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun onArtist(entity: Entities, tag: String) = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun onArtist(entity: Entities) = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun onVenue(entity: Entities, tag: String) = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun onVenue(entity: Entities) = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun onAction(entity: Entities) = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun onAction(action: String) = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}

	fun onAction() = run {
		Logger.print(
			"Action performed but not implemented"
		)
	}
}