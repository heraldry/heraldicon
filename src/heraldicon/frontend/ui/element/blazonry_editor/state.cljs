(ns heraldicon.frontend.ui.element.blazonry-editor.state
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

(defn selection ^draft-js/SelectionState [^draft-js/EditorState state]
  (.getSelection state))

(defn content ^draft-js/ContentState [^draft-js/EditorState state]
  (.getCurrentContent state))

(defn cursor-index ^js/Number [^draft-js/EditorState state]
  (let [selection (selection state)
        content (content state)
        block ^draft-js/ContentBlock (.getBlockForKey content (.getFocusKey selection))
        block-start (block-start-index content block)
        offset (.getFocusOffset selection)]
    (+ block-start offset)))

(defn- merge-selection [selection other]
  (.merge selection (clj->js other)))

(defn set-cursor-index ^draft-js/EditorState [^draft-js/EditorState state index]
  (let [content (content state)
        selection (selection state)
        {:keys [key offset]} (get-block-key-and-offset content index)
        selection (merge-selection selection
                                   {:anchorKey key
                                    :anchorOffset offset
                                    :focusKey key
                                    :focusOffset offset})]
    (draft-js/EditorState.forceSelection state selection)))

(defn text ^js/String [^draft-js/EditorState state]
  (.getPlainText (content state)))

(defn set-text ^draft-js/EditorState [^draft-js/EditorState state text]
  (let [new-content (draft-js/ContentState.createFromText text)]
    (-> (draft-js/EditorState.push state new-content "insert-characters")
        (set-cursor-index (count text)))))

(defn replace-text ^draft-js/EditorState [^draft-js/EditorState state from-index to-index text]
  (let [content (content state)
        {start-key :key
         start-offset :offset} (get-block-key-and-offset content from-index)
        {end-key :key
         end-offset :offset} (get-block-key-and-offset content to-index)
        range-selection (merge-selection (selection state)
                                         {:anchorKey start-key
                                          :anchorOffset start-offset
                                          :focusKey end-key
                                          :focusOffset end-offset})
        new-content (draft-js/Modifier.replaceText content range-selection text)]
    (-> (draft-js/EditorState.push state new-content "insert-characters")
        (set-cursor-index (+ (count text) from-index)))))

(defn highlight-unknown-string ^draft-js/EditorState [^draft-js/EditorState state index]
  (draft-js/EditorState.set state (clj->js {:decorator (unknown-string-decorator index)})))

(defn create []
  (.createEmpty draft-js/EditorState))
