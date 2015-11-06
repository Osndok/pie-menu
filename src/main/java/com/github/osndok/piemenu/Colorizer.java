package com.github.osndok.piemenu;

/**
 * Created by robert on 2015-11-06 01:11.
 */
public
enum Colorizer
{
	/**
	 * For those that are of the opinion that distinct & separate colors make for quicker and easier identification,
	 * we have this (the default) mechanism that will generally assign dissimilar colors to adjacent wedges. Note that
	 * removing elements may cause two adjacent wedges to have a similar color, but will not re-color any pre-existing
	 * wedges (i.e. avoids visual jarring).
	 */
	DISTINCT_HUES,

	/**
	 * For those that are of the opinion that the aesthetics of a smoothly transitioning array of colors is more
	 * important than any functional gain of having separated colors, we have this (the backup) mechanism that
	 * will generally assign very-similar (yet distinct) colors to adjacent wedges. When all the menu's elements
	 * are available at the time of first painting, this will generally result in a standard 'color wheel', or
	 * (in the case of multiple rings) a 'color spiral', but if elements are added or removed after the element is
	 * first painted, then gaps and non-uniformity in the coloration will likely occur.
	 */
	GRADIENT,

	/**
	 * For those that desire a more stable long-term coloration, but are unwilling or unable to provide a specific
	 * color for each wedge... this mechanism will select a color that is deterministically chosen from the string
	 * value of the label itself. Such a color choice is expected to be stable across VM restarts, and ameliorate
	 * user frustration of menu items being "moved around by developers" (because they remain the same color).
	 *
	 * Furthermore, (if used system-wide) this could have an interesting but subtle effect on UX... e.g. that all
	 * the 'Copy' menu items will be the same color across programs, and that the user might be able to find it more
	 * quickly in a new program.
	 *
	 * The original use-case for this colorization scheme was intended for the rather demanding requirements for
	 * menu colors that are:
	 * (1) heavily accessed as part of a user's daily workflow (important & long-term),
	 * (2) likely to change (however infrequently; not perfectly stable),
	 * (3) derived from user-provided strings, and
	 * (4) likely to "cross-pollinate" to other machines/systems/interfaces ("follows" users), and
	 * (5) likely to spontaneously agree with data derived from another user (mutual "discovery" of the same color).
	 */
	LABEL_HASH
}
