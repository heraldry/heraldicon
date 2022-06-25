(ns heraldicon.frontend.ui.element.blazonry-editor.editor-state
  (:require
   ["draft-js" :as draft-js]
   [reagent.core :as r]))

(defn- block-start-index [^draft-js/ContentState content
                          ^draft-js/ContentBlock block]
  (->> content
       .getBlocksAsArray
       (take-while #(not= (.-key %) (.-key block)))
       (map (fn [^draft-js/ContentBlock block]
              (.getLength block)))
       (reduce +)))

(defn- get-block-key-and-offset [^draft-js/ContentState content
                                 index]
  (loop [[^draft-js/ContentBlock block & rest] (.getBlocksAsArray content)
         index index]
    (when block
      (let [block-length (.getLength block)]
        (if (<= index block-length)
          {:key (.getKey block)
           :offset index}
          (recur rest (- index block-length)))))))

(defn- unknown-string-decorator [index]
  (draft-js/CompositeDecorator.
   (clj->js
    [{:strategy (fn [^draft-js/ContentBlock block
                     callback
                     ^draft-js/ContentState content]
                  (when index
                    (let [block-start (block-start-index content block)
                          block-end (+ block-start (.getLength block))]
                      (when (<= index block-end)
                        (callback (-> index
                                      (max block-start)
                                      (- block-start))
                                  (- block-end
                                     block-start))))))
      :component (fn [props]
                   (r/as-element [:span {:style {:color "red"}} (.-children props)]))}])))

(defprotocol EditorStateProtocol
  (selection ^:private [this] "Get selection")
  (content ^:private [this] "Get content")
  (text [this] "Get text")
  (set-text [this text] "Set text content")
  (replace-text [this from-index to-index text] "Replace text for the given range")
  (cursor-index [this] "Get the cursor index")
  (set-cursor-index [this index] "Set the cursor index")
  (highlight-unknown-string [this index] "Highlight the unknown string"))

(defrecord ^:export EditorState [^draft-js/EditorState state]
  EditorStateProtocol

  (selection ^draft-js/SelectionState [{:keys [state]}]
    (.getSelection state))

  (content ^draft-js/ContentState [{:keys [state]}]
    (.getCurrentContent state))

  (text ^js/String [this]
    (.getPlainText (content this)))

  (set-text ^EditorState [{:keys [state]} text]
    (let [new-content (draft-js/ContentState.createFromText text)]
      (-> (EditorState. (draft-js/EditorState.push state new-content "insert-characters"))
          (set-cursor-index (count text)))))

  (replace-text ^EditorState [{:keys [state] :as this} from-index to-index text]
    (let [content (content this)
          {start-key :key
           start-offset :offset} (get-block-key-and-offset content from-index)
          {end-key :key
           end-offset :offset} (get-block-key-and-offset content to-index)
          range-selection {:anchorKey start-key
                           :anchorOffset start-offset
                           :focusKey end-key
                           :focusOffset end-offset}
          range-selection (.merge (selection this) (clj->js range-selection))
          new-content (draft-js/Modifier.replaceText content range-selection text)]
      (-> (EditorState. (draft-js/EditorState.push state new-content "insert-characters"))
          (set-cursor-index (+ (count text) from-index)))))

  (cursor-index ^js/Number [this]
    (let [selection (selection this)
          content (content this)
          block (->> selection
                     .getFocusKey
                     ^draft-js/ContentBlock (.getBlockForKey content))
          block-start (block-start-index content block)
          offset (.getFocusOffset selection)]
      (+ block-start offset)))

  (set-cursor-index ^EditorState [{:keys [state] :as this} index]
    (let [content (content this)
          selection (selection this)
          {:keys [key offset]} (get-block-key-and-offset content index)
          selection (.merge selection
                            (clj->js {:anchorKey key
                                      :anchorOffset offset
                                      :focusKey key
                                      :focusOffset offset}))]
      (EditorState. (draft-js/EditorState.forceSelection state selection))))

  (highlight-unknown-string ^EditorState [{:keys [state]} index]
    (EditorState.
     (draft-js/EditorState.set state (clj->js {:decorator (unknown-string-decorator index)})))))

(defn create []
  (EditorState. (.createEmpty draft-js/EditorState)))
