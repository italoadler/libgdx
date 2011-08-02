/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.utils.AndroidClipboard;
import com.badlogic.gdx.scenes.scene2d.ui.utils.Clipboard;
import com.badlogic.gdx.scenes.scene2d.ui.utils.DesktopClipboard;
import com.badlogic.gdx.utils.FloatArray;

/**
 * A single-line text field.
 * 
 * <h2>Functionality</h2>
 * A TextField provides a way to get single-line text input from a user. It supports
 * scrolling based on the cursor position, selection via holding down the shift-key (desktop-only),
 * and copy & paste (desktop only).
 * 
 * <h3>Copy & Paste</h3>
 * The TextField will copy the currently selected text when ctrl + c is pressed, and paste any
 * text in the clipboard when ctrl + v is pressed. Clipboard functionality is provided via the {@link Clipboard}
 * interface. Currently there are two standard implementations, one for the desktop and one for Android. The later
 * is a stub, as copy & pasting on Android is not supported yet. To set your own Clipboard implementation use the
 * {@link #setClipboard(Clipboard)} method.
 * 
 * <h3>On-Screen Keyboard</h3>
 * The TextField allows you to specify an {@link OnscreenKeyboard} implementation responsible for displaying a 
 * softkeyboard and piping all key events generated by the keyboard to the TextField. There are two standard
 * implementations, one for the desktop and one for Android. The former is a stub, as a softkeyboard is not needed
 * on the desktop. The Android {@link OnscreenKeyboard} implementation will bring up the default IME. If you want
 * to set your own {@link OnscreenKeyboard} for a TextField use the {@link #setOnscreenKeyboard(OnscreenKeyboard)} 
 * method. The OnscreenKeyboard allows you to dynamically show and hide it, e.g. based on a key event for the return
 * key.
 * 
 * <h3>Listening to Key Events<h3>
 * To listen to TextField events one can register a {@link TextFieldListener}. This listener will be invoked after
 * a new character was added or removed to and from the TextField. This allows you to perform input checks or dynamically
 * hide the OnscreenKeyboard.
 * 
 * <h2>Layout</h2>
 * The (preferred) width and height of a TextField are derrived from the width given at construction
 * time as well as the combination of the used font's height and the top and bottom border patches. Use 
 * {@link Button#setPrefSize(int, int)} to programmatically change the size to your liking. In case the 
 * width and height you set are to small for the contained text, the TextField will clip the characters
 * based on the current cursor position.
 * 
 * <h2>Style</h2>
 * A TextField is a {@link Widget} displaying a background {@link NinePatch}, the current text via a 
 * {@link BitmapFont} and {@link Color}, a cursor via a {@link NinePatch} as well as the current selection
 * via a {@link TextureRegion} that is stretched over the entire selection. The style is defined via an instance
 * of {@link TextFieldStyle}, which can be either done programmatically or via a {@link Skin}.</p>
 * 
 * A TextField's style definition in a skin XML file should look like this:
 * 
 * <pre>
 * {@code 
 * <textfield name="name" 
 *            font="fontName" 
 *            fontColor="fontColor" 
 *            cursor="cursorPatch" 
 *            selection="selectionRegion" 
 *            background="backgroundPatch"/>
 * }
 * </pre>
 * 
 * <ul>
 * <li>The <code>name</code> attribute defines the name of the style which you can later use with {@link Skin#newTextField(String, float, String)}.</li>
 * <li>The <code>font</code> attribute references a {@link BitmapFont} by name, to be used to render the text in the text field</li>
 * <li>The <code>fontColor</code> attribute references a {@link Color} by name, to be used to render the text on the text field</li>
 * <li>The <code>cursorPatch</code> attribute references a {@link NinePatch} by name, to be used to render the text field's cursor</li>
 * <li>The <code>selectionRegion</code> attribute references a {@link TextureRegion} by name, to be used to highlight the text field's selection</li>
 * <li>The <code>backgroundPatch</code> attribute references a {@link NinePatch} by name, to be used as the text field's background</li>
 * </ul> 
 * 
 * @author mzechner
 *
 */
public class TextField extends Widget {
	final TextFieldStyle style;
	
	Clipboard clipboard;	
	final Rectangle fieldBounds = new Rectangle();
	final TextBounds textBounds = new TextBounds();
	final Rectangle scissor = new Rectangle();
	
	TextFieldListener listener;
	String text = "";
	int cursor = 0;
	float renderOffset = 0;
	float textOffset = 0;
	int visibleTextStart = 0;
	int visibleTextEnd = 0;
	final StringBuilder builder = new StringBuilder();
	final FloatArray glyphAdvances = new FloatArray();
	final FloatArray glyphPositions = new FloatArray();
	final float initialPrefWidth;
	float blinkTime = 0.5f;
	long lastBlink = System.nanoTime();
	boolean cursorOn = true;
	boolean hasSelection = false;
	int selectionStart = 0;
	float selectionX = 0;
	float selectionWidth = 0;
	OnscreenKeyboard keyboard = new DefaultOnscreenKeyboard();

	/**
	 * Creates a new Textfield. The width is determined by the prefWidth
	 * parameter, the height is determined by the font's height as well
	 * as the top and bottom border patches of the text fields background.
	 * @param name the name
	 * @param prefWidth the (preferred) width
	 * @param style the {@link TextFieldStyle}
	 */
	public TextField (String name, float prefWidth, TextFieldStyle style) {
		super(name, prefWidth, 0);
		this.style = style;
		this.initialPrefWidth = prefWidth;
		this.clipboard = Clipboard.getDefaultClipboard();
		layout();
		this.width = this.prefWidth;
		this.height = this.prefHeight;
	}

	@Override public void layout () {
		final BitmapFont font = style.font;
		final NinePatch background = style.background;
		
		textBounds.set(font.getBounds(text));
		textBounds.height -= font.getDescent() * 2;
		font.computeGlyphAdvancesAndPositions(text, glyphAdvances, glyphPositions);

		prefHeight = background.getBottomHeight() + background.getTopHeight() + textBounds.height;
		prefWidth = background.getLeftWidth() + background.getRightWidth() + initialPrefWidth;
		invalidated = false;
	}

	private void blink () {
		long time = System.nanoTime();
		if ((time - lastBlink) / 1000000000.0f > blinkTime) {
			cursorOn = !cursorOn;
			lastBlink = time;
		}
	}

	private void calculateOffsets () {		
		final NinePatch background = style.background;
		
		float position = glyphPositions.get(cursor);
		float distance = position - Math.abs(renderOffset);
		float visibleWidth = width - background.getLeftWidth() - background.getRightWidth();

		// check whether the cursor left the left or right side of
		// the visible area and adjust renderoffset.
		if (distance <= 0) {
			if (cursor > 0)
				renderOffset = -glyphPositions.get(cursor - 1);
			else
				renderOffset = 0;
		} else {
			if (distance > visibleWidth) {
				renderOffset -= distance - visibleWidth;
			}
		}

		// calculate first visible char based on render offset
		visibleTextStart = 0;
		textOffset = 0;
		float start = Math.abs(renderOffset);
		int len = glyphPositions.size;
		float startPos = 0;
		for (int i = 0; i < len; i++) {
			if (glyphPositions.items[i] >= start) {
				visibleTextStart = i;
				startPos = glyphPositions.items[i];
				textOffset = glyphPositions.items[visibleTextStart] - start;
				break;
			}
		}

		// calculate last visible char based on visible width and render offset		
		visibleTextEnd = Math.min(text.length(), cursor + 1);
		for (; visibleTextEnd <= text.length(); visibleTextEnd++) {
			if (glyphPositions.items[visibleTextEnd] - startPos > visibleWidth) break;
		}
		visibleTextEnd = Math.max(0, visibleTextEnd -1);
		
		// calculate selection x position and width
		if(hasSelection) {
			int minIndex = Math.min(cursor, selectionStart);
			int maxIndex = Math.max(cursor, selectionStart);
			float minX = Math.max(glyphPositions.get(minIndex), glyphPositions.get(visibleTextStart));
			float maxX = Math.min(glyphPositions.get(maxIndex), glyphPositions.get(visibleTextEnd));
			selectionX = minX;
			selectionWidth = maxX - minX;
		}
	}

	@Override protected void draw (SpriteBatch batch, float parentAlpha) {
		final BitmapFont font = style.font;
		final Color fontColor = style.fontColor;
		final NinePatch background = style.background;
		final TextureRegion selection = style.selection;
		final NinePatch cursorPatch = style.cursor;
		
		if (invalidated) layout();

		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		background.draw(batch, x, y, width, height);
		float textY = (int)(height / 2) + (int)(textBounds.height / 2) + font.getDescent() / 2;
		font.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * parentAlpha);
		calculateOffsets();

		if (hasSelection) {
			batch.draw(selection, x + selectionX + background.getLeftWidth() + renderOffset,
				y + textY - textBounds.height - font.getDescent() / 2, selectionWidth, textBounds.height);
		}

		font.draw(batch, text, x + background.getLeftWidth() + textOffset, y + textY, visibleTextStart, visibleTextEnd);
		if (parent.keyboardFocusedActor == this) {
			blink();
			if (cursorOn) {
				cursorPatch.draw(batch, x + background.getLeftWidth() + glyphPositions.get(cursor) + renderOffset - 1, y + textY
					- textBounds.height - font.getDescent() / 2, cursorPatch.getTotalWidth(), textBounds.height);
			}
		}
	}

	@Override public boolean touchDown (float x, float y, int pointer) {
		if (pointer != 0) return false;
		if (hit(x, y) != null) {
			parent.keyboardFocus(this);
			keyboard.show(true);
			x = x - renderOffset;
			for(int i = 0; i < glyphPositions.size; i++) {
				float pos = glyphPositions.items[i];
				if(pos > x) {
					cursor = Math.max(0,i-1);
					break;
				}
			}
			return true;
		} else
			return false;
	}

	@Override public boolean touchUp (float x, float y, int pointer) {
		return false;
	}

	@Override protected boolean touchDragged (float x, float y, int pointer) {
		return false;
	}

	protected boolean keyDown (int keycode) {
		final BitmapFont font = style.font;
		
		if (parent.keyboardFocusedActor == this) {			
			// clipboard
			if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT)) {
				if (keycode == Keys.V) {
					String content = clipboard.getContents();
					if (content != null) {
						StringBuilder builder = new StringBuilder();
						for (int i = 0; i < content.length(); i++) {
							char c = content.charAt(i);
							if (font.containsCharacter(c)) {
								builder.append(c);
							}
						}
						content = builder.toString();
						text = text.substring(0, cursor) + content + text.substring(cursor, text.length());
						cursor += content.length();
						font.computeGlyphAdvancesAndPositions(text, glyphAdvances, glyphPositions);
					}
				}
				if (keycode == Keys.C) {
					if (hasSelection) {
						int minIndex = Math.min(cursor, selectionStart);
						int maxIndex = Math.max(cursor, selectionStart);
						clipboard.setContents(text.substring(minIndex, maxIndex));
					}
				}
			}
			// selection
			else if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)) {
				if (keycode == Keys.LEFT) {
					if(!hasSelection) {
						selectionStart = cursor;
						hasSelection = true;
					}
					cursor--;
				}
				if (keycode == Keys.RIGHT) {
					if(!hasSelection) {
						selectionStart = cursor;
						hasSelection = true;
					}
					cursor++;
				}
				if (keycode == Keys.HOME) {
					if(!hasSelection) {
						selectionStart = cursor;
						hasSelection = true;
					}
					cursor = 0;
				}
				if (keycode == Keys.END) {
					if(!hasSelection) {
						selectionStart = cursor;
						hasSelection = true;
					}
					cursor = text.length();
				}

				cursor = Math.max(0, cursor);
				cursor = Math.min(text.length(), cursor);
			}
			// cursor movement or other keys (kill selection)
			else {
				if (keycode == Keys.LEFT) {
					cursor--;
					hasSelection = false;
				}
				if (keycode == Keys.RIGHT) {
					cursor++;
					;
					hasSelection = false;
				}
				if (keycode == Keys.HOME) {
					cursor = 0;
					hasSelection = false;
				}
				if (keycode == Keys.END) {
					cursor = text.length();
					hasSelection = false;
				}

				cursor = Math.max(0, cursor);
				cursor = Math.min(text.length(), cursor);
			}

			return true;
		}
		return false;
	}

	protected boolean keyTyped (char character) {
		final BitmapFont font = style.font;
		
		if (parent.keyboardFocusedActor == this) {			
			if(character == 8 && (cursor > 0 || hasSelection)) {				
				if(!hasSelection) {
					text = text.substring(0, cursor - 1) + text.substring(cursor);
					cursor--;
					font.computeGlyphAdvancesAndPositions(text, glyphAdvances, glyphPositions);
				} else {
					int minIndex = Math.min(cursor, selectionStart);
					int maxIndex = Math.max(cursor, selectionStart);
					
					text = (minIndex>0?text.substring(0, minIndex):"") + (maxIndex < text.length()?text.substring(maxIndex, text.length()):"");
					cursor = minIndex;
					font.computeGlyphAdvancesAndPositions(text, glyphAdvances, glyphPositions);
					hasSelection = false;
				}
			}
			if(character == 127 && (cursor < text.length() || hasSelection)) {				
				if(!hasSelection) {
					text = text.substring(0, cursor) + text.substring(cursor + 1);					
					font.computeGlyphAdvancesAndPositions(text, glyphAdvances, glyphPositions);
				} else {
					int minIndex = Math.min(cursor, selectionStart);
					int maxIndex = Math.max(cursor, selectionStart);
					
					text = (minIndex>0?text.substring(0, minIndex):"") + (maxIndex < text.length()?text.substring(maxIndex, text.length()):"");
					cursor = minIndex;
					font.computeGlyphAdvancesAndPositions(text, glyphAdvances, glyphPositions);
					hasSelection = false;
				}
			}			
			if (font.containsCharacter(character)) {
				if(!hasSelection) {
					text = text.substring(0, cursor) + character + text.substring(cursor, text.length());
					cursor++;
					font.computeGlyphAdvancesAndPositions(text, glyphAdvances, glyphPositions);
				} else {
					int minIndex = Math.min(cursor, selectionStart);
					int maxIndex = Math.max(cursor, selectionStart);
					
					text = (minIndex>0?text.substring(0, minIndex):"") + (maxIndex < text.length()?text.substring(maxIndex, text.length()):"");
					cursor = minIndex;
					text = text.substring(0, cursor) + character + text.substring(cursor, text.length());
					cursor++;
					font.computeGlyphAdvancesAndPositions(text, glyphAdvances, glyphPositions);
					hasSelection = false;
				}
			}
			if(listener != null) listener.keyTyped(this, character);
			return true;
		} else
			return false;
	}

	@Override public Actor hit (float x, float y) {
		return x > 0 && x < width && y > 0 && y < height ? this : null;
	}

	/**
	 * Defines a text field's style, see {@link TextField}
	 * @author mzechner
	 *
	 */
	public static class TextFieldStyle {
		public final NinePatch background;
		public final BitmapFont font;
		public final Color fontColor;		
		public final NinePatch cursor;
		public final TextureRegion selection;

		public TextFieldStyle (BitmapFont font, Color fontColor, NinePatch cursor, TextureRegion selection, NinePatch background) {
			this.background = background;
			this.cursor = cursor;
			this.font = font;
			this.fontColor = fontColor;
			this.selection = selection;
		}
	}
	
	/**
	 * Interface for listening to typed characters.
	 * @author mzechner
	 *
	 */
	public interface TextFieldListener {
		public void keyTyped(TextField textField, char key);
	}
	
	/**
	 * Sets the {@link TextFieldListener}
	 * @param listener the listener or null
	 */
	public void setTextFieldListener(TextFieldListener listener) {
		this.listener = listener;
	}

	/**
	 * Sets the text of this text field.
	 * @param text the text
	 */
	public void setText (String text) {
		final BitmapFont font = style.font;
		
		if (text == null) throw new IllegalArgumentException("text must not be null");
		this.text = text;
		this.cursor = 0;
		this.hasSelection = false;		
		font.computeGlyphAdvancesAndPositions(text, this.glyphAdvances, this.glyphPositions);
		invalidateHierarchy();
	}

	/**
	 * @return the text of this text field. Never null, might be an empty string.
	 */
	public String getText () {
		return text;
	}
	
	/**
	 * Returns the currently used {@link OnscreenKeyboard}. {@link TextField} instances
	 * use the {@link DefaultOnscreenKeyboard} by default.
	 * @return the OnscreenKeyboard.
	 */
	public OnscreenKeyboard getOnscreenKeyboard() {
		return keyboard;
	}
	
	/**
	 * Sets the {@link OnscreenKeyboard} to be used by this textfield
	 * @param keyboard the OnscreenKeyboard
	 */
	public void setOnscreenKeyboard(OnscreenKeyboard keyboard) {
		this.keyboard = keyboard;
	}
	
	/**
	 * An interface for onscreen keyboards. Can invoke the 
	 * default keyboard or render your own keyboard!
	 * @author mzechner
	 *
	 */
	public interface OnscreenKeyboard {
		public void show(boolean visible);		
	}
	
	/**
	 * The default {@link OnscreenKeyboard} used by all {@link TextField} instances.
	 * Just uses {@link Input#setOnscreenKeyboardVisible(boolean)} as appropriate. Might
	 * overlap your actual rendering, so use with care!
	 * @author mzechner
	 *
	 */
	public static class DefaultOnscreenKeyboard implements OnscreenKeyboard {
		@Override public void show (boolean visible) {
			Gdx.input.setOnscreenKeyboardVisible(visible);
		}		
	}
	
	/**
	 * Sets the {@link Clipboard} implementation this TextField uses.
	 * @param clipboard the Clipboard
	 */
	public void setClipboard(Clipboard clipboard) {
		this.clipboard = clipboard;
	}
}