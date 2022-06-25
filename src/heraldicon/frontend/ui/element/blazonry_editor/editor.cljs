(ns heraldicon.frontend.ui.element.blazonry-editor.editor
  (:require
   ["draft-js" :as draft-js]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.debounce :as debounce]
   [heraldicon.frontend.ui.element.blazonry-editor.parser :as parser]
   [heraldicon.frontend.ui.element.blazonry-editor.shared :as shared]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private editor-state-path
  (conj shared/blazonry-editor-path :editor-state))

(def ^:private last-parsed-path
  (conj shared/blazonry-editor-path :last-parsed))

(rf/reg-event-db ::clear
  (fn [db _]
    (-> db
        (assoc-in editor-state-path (.createEmpty draft-js/EditorState))
        #_(assoc-in status-path nil)
        (assoc-in last-parsed-path nil))))

(defn- caret-position [index]
  (let [selection (js/document.getSelection)
        range-count (.-rangeCount selection)]
    (when (pos? range-count)
      (let [range (.getRangeAt selection 0)
            node (.-startContainer range)
            node-length (-> node .-length (or 0))
            offset (if index
                     (min index node-length)
                     (.-startOffset range))]
        (cond
          (pos? offset) (let [rect (.getBoundingClientRect
                                    (doto (js/document.createRange)
                                      (.setStart node (dec offset))
                                      (.setEnd node offset)))]
                          {:top (.-top rect)
                           :left (.-left rect)})
          (< offset
             node-length) (let [rect (.getBoundingClientRect
                                      (doto (js/document.createRange)
                                        (.setStart node offset)
                                        (.setEnd node (inc offset))))]
                            {:top (.-top rect)
                             :left (.-left rect)})
          :else (let [node (first (js/document.getElementsByClassName "DraftEditor-root"))
                      rect (.getBoundingClientRect node)
                      styles (js/getComputedStyle node)
                      line-height (js/parseInt (.-lineHeight styles))
                      font-size (js/parseInt (.-fontSize styles))
                      delta (/ (- line-height font-size) 2)]
                  {:top (-> rect
                            .-top
                            (+ delta))
                   :left (.-left rect)}))))))

(defn- get-block-key-and-offset [^draft-js/ContentState content
                                 index]
  (loop [[^draft-js/ContentBlock block & rest] (.getBlocksAsArray content)
         index index]
    (when block
      (let [block-length (.getLength block)]
        (if (<= index block-length)
          {:key (.getKey block)
           :offset index}
          (recur rest
                 (- index block-length)))))))

(defn- block-start-index [^draft-js/ContentState content block]
  (->> content
       .getBlocksAsArray
       (take-while #(not= (.-key %) (.-key block)))
       (map (fn [^draft-js/ContentBlock block]
              (.getLength block)))
       (reduce +)))

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

(defn- cursor-index [^draft-js/EditorState editor-state]
  (let [selection ^draft-js/Selection (.getSelection editor-state)
        content ^draft-js/ContentState (.getCurrentContent editor-state)
        block (->> selection
                   .getFocusKey
                   ^draft-js/ContentBlock (.getBlockForKey content))
        block-start (block-start-index content block)
        offset (.getFocusOffset selection)]
    (+ block-start offset)))

(rf/reg-event-fx ::parse-if-changed
  (fn [{:keys [db]} _]
    (let [editor-state ^draft-js/EditorState (get-in db editor-state-path)
          content ^draft-js/ContentState (.getCurrentContent editor-state)
          last-parsed (get-in db last-parsed-path)
          text (.getPlainText content)]
      (cond-> {}
        (not= text last-parsed) (assoc :dispatch [::parser/parse text (cursor-index editor-state)])))))

(rf/reg-event-fx ::parse-result
  (fn [{:keys [db]} [_ result]]
    (let [editor-state ^draft-js/EditorState (get-in db editor-state-path)
          content ^draft-js/ContentState (.getCurrentContent editor-state)
          last-parsed (get-in db last-parsed-path)
          text (.getPlainText content)]
      (cond-> {}
        (not= text last-parsed) (assoc :dispatch [::parser/parse text (cursor-index editor-state)])))))

(def ^:private change-dedupe-time
  250)

(rf/reg-event-fx ::update-editor-state
  (fn [{:keys [db]} [_ editor-state]]
    {:db (assoc-in db editor-state-path editor-state)
     ::debounce/dispatch [::debounce-update-editor-state [::parse-if-changed] change-dedupe-time]}))

(defn- put-cursor-at [^draft-js/EditorState state index]
  (let [content (.getCurrentContent state)
        {:keys [key offset]} (get-block-key-and-offset content index)
        selection (-> state
                      ^draft-js/SelectionState (.getSelection)
                      (.merge (clj->js {:anchorKey key
                                        :anchorOffset offset
                                        :focusKey key
                                        :focusOffset offset})))]
    (draft-js/EditorState.forceSelection
     state
     selection)))

(rf/reg-event-fx ::set-blazon
  (fn [{:keys [db]} [_ blazon]]
    (let [state ^draft-js/EditorState (get-in db editor-state-path)
          new-content (draft-js/ContentState.createFromText blazon)]
      {:dispatch [::update-editor-state (-> state
                                            (draft-js/EditorState.push
                                             new-content
                                             "insert-characters")
                                            (put-cursor-at (count blazon)))]})))

(rf/reg-event-fx ::auto-completion-clicked
  (fn [{:keys [db]} [_ index cursor-index choice]]
    (let [state (get-in db editor-state-path)
          content ^draft-js/ContentState (.getCurrentContent state)
          current-text (.getPlainText content)
          choice (cond-> choice
                   (and (pos? index)
                        (pos? (count current-text))
                        (not= (subs current-text (dec index) index) " ")) (->> (str " "))
                   (and (< cursor-index (count current-text))
                        (not= (subs current-text cursor-index (inc cursor-index)) " ")) (str " "))
          {start-key :key
           start-offset :offset} (get-block-key-and-offset content index)
          {end-key :key
           end-offset :offset} (get-block-key-and-offset content cursor-index)
          range-selection {:anchorKey start-key
                           :anchorOffset start-offset
                           :focusKey end-key
                           :focusOffset end-offset}
          range-selection (-> state
                              ^draft-js/SelectionState (.getSelection)
                              (.merge (clj->js range-selection)))
          new-content (draft-js/Modifier.replaceText
                       content
                       range-selection
                       choice)
          new-state (-> state
                        (draft-js/EditorState.push
                         new-content
                         "insert-characters")
                        (put-cursor-at (+ (count choice) index)))]
      {:fx [[:dispatch [::auto-complete/clear]]
            [:dispatch [::update-editor-state new-state]]]})))

(defn editor []
  [:div {:style {:display "inline-block"
                 :outline "1px solid black"
                 :width "100%"
                 :height "100%"}}
   [(r/create-class
     {:display-name "core"
      :reagent-render (fn []
                        (let [state @(rf/subscribe [:get editor-state-path])]
                          [:> draft-js/Editor
                           {:editorState state
                            :onChange #(rf/dispatch-sync [::update-editor-state %])
                            :keyBindingFn (fn [event]
                                            (if (= (.-code event) "Tab")
                                              (do
                                                (.preventDefault event)
                                                (.stopPropagation event)
                                                "auto-complete")
                                              (draft-js/getDefaultKeyBinding event)))
                            :handleKeyCommand (fn [command]
                                                (if (= command "auto-complete")
                                                  (do
                                                    (rf/dispatch [::auto-complete/apply-first])
                                                    "handled")
                                                  "not-handled"))}]))})]])
