(ns heraldry.frontend.ui.element.blazonry-editor
  (:require
   ["draft-js" :as draft-js]
   ["genex" :as genex]
   ["html-entities" :as html-entities]
   [clojure.string :as s]
   [heraldry.blazonry.parser :as blazonry-parser]
   [heraldry.frontend.auto-complete :as auto-complete]
   [heraldry.frontend.context :as context]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.modal :as modal]
   [heraldry.render :as render]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def blazon-editor-path
  [:ui :blazon-editor])

(def hdn-path
  (conj blazon-editor-path :arms-form))

(def editor-state-path
  (conj blazon-editor-path :editor-state))

(defn caret-position [index]
  (let [selection (js/document.getSelection)
        range-count (.-rangeCount selection)]
    (when (pos? range-count)
      (let [range (.getRangeAt selection 0)
            node (.-startContainer range)
            offset (if index
                     (min index (.-length node))
                     (.startOffset range))]
        (cond
          (pos? offset) (let [rect (-> (doto (js/document.createRange)
                                         (.setStart node (dec offset))
                                         (.setEnd node offset))
                                       .getBoundingClientRect)]
                          {:top (.-top
                                 rect)
                           :left (.-left rect)})
          (< offset (.-length node)) (let [rect (-> (doto (js/document.createRange)
                                                      (.setStart node offset)
                                                      (.setEnd node (inc offset)))
                                                    .getBoundingClientRect)]
                                       {:top (.-top rect)
                                        :left (.-left rect)})
          :else (let [rect (.getBoundingClientRect node)
                      styles (js/getComputedStyle node)
                      line-height (js/parseInt (.-lineHeight styles))
                      font-size (js/parseInt (.-lineSize styles))
                      delta (/ (- line-height font-size) 2)]
                  {:top (-> rect
                            .-top
                            (+ delta))
                   :left (.-left rect)}))))))

(defn parse-blazonry [value cursor-index]
  (try
    (let [hdn (blazonry-parser/blazon->hdn value)]
      {:value value
       :hdn hdn})
    (catch :default e
      (let [{:keys [reason
                    index]} (ex-data e)
            parsed (subs value 0 index)
            problem (subs value index)
            cursor-index (-> cursor-index
                             (max index)
                             (min (count value)))
            typed-string (subs value index cursor-index)
            auto-complete-choices (->> reason
                                       (mapcat (fn [{:keys [tag expecting]}]
                                                 (case tag
                                                   :optional []
                                                   :string [expecting]
                                                   :regexp (-> expecting
                                                               genex
                                                               .generate
                                                               js->clj))))
                                       (filter (fn [choice]
                                                 (if choice
                                                   (s/starts-with? choice typed-string)
                                                   true)))
                                       sort
                                       dedupe
                                       vec)
            position (caret-position index)]
        {:value value
         :html [:span (html-entities/encode parsed)
                [:span {:style {:color "red"}} (html-entities/encode problem)]]
         :auto-complete (cond-> {:choices auto-complete-choices
                                 :on-click (fn [choice]
                                             (rf/dispatch [:auto-completion-clicked index cursor-index choice]))}
                          position (assoc :position position))
         :index index}))))

(defn get-block-key-and-offset [^draft-js/ContentState content
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

(defn block-start-index [^draft-js/ContentState content block]
  (->> content
       .getBlocksAsArray
       (take-while #(not= (.-key %) (.-key block)))
       (map (fn [^draft-js/ContentBlock block]
              (.getLength block)))
       (reduce +)))

(defn unknown-string-decorator [index]
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

(defn cursor-index [^draft-js/EditorState editor-state]
  (let [selection ^draft-js/Selection (.getSelection editor-state)
        content ^draft-js/ContentState (.getCurrentContent editor-state)
        block (->> selection
                   .getFocusKey
                   ^draft-js/ContentBlock (.getBlockForKey content))
        block-start (block-start-index content block)
        offset (.getFocusOffset selection)]
    (+ block-start offset)))

(defn on-editor-change [^draft-js/EditorState new-editor-state]
  (let [content ^draft-js/ContentState (.getCurrentContent new-editor-state)
        text (.getPlainText content)
        cursor-index (cursor-index new-editor-state)
        {:keys [hdn
                auto-complete
                index]} (parse-blazonry text cursor-index)
        new-editor-state (draft-js/EditorState.set
                          new-editor-state
                          (clj->js
                           {:decorator (unknown-string-decorator index)}))]
    (auto-complete/set-data auto-complete)
    (when hdn
      (rf/dispatch [:set hdn-path {:coat-of-arms {:field hdn}
                                   :render-options {:outline? true}}]))
    (rf/dispatch [:set editor-state-path new-editor-state])))

(defn put-cursor-at [^draft-js/EditorState state index]
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

(macros/reg-event-db :auto-completion-clicked
  (fn [db [_ index cursor-index choice]]
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
                        (put-cursor-at (-> (count choice)
                                           (+ index))))]
      (on-editor-change new-state)
      (auto-complete/clear-data)
      db)))

(defn blazonry-editor [attributes]
  [:div attributes
   [(r/create-class
     {:display-name "core"
      :reagent-render (fn []
                        (let [state @(rf/subscribe [:get editor-state-path])]
                          [:> draft-js/Editor
                           {:editorState state
                            :onChange on-editor-change}]))})]])

(defn open [context]
  (rf/dispatch-sync [:set editor-state-path (.createEmpty draft-js/EditorState)])
  (rf/dispatch-sync [:set hdn-path {:coat-of-arms {:field {:type :heraldry.field.type/plain
                                                           :tincture :none}}
                                    :render-options {:outline? true}}])
  (modal/create
   :string.button/from-blazon
   [(fn []
      [:div {:style {:width "40em"
                     :height "20em"}}
       [blazonry-editor
        {:style {:display "inline-block"
                 :outline "1px solid black"
                 :width "calc(60% - 10px)"
                 :height "20em"}}]
       [:div {:style {:display "inline-block"
                      :width "40%"
                      :height "100%"
                      :float "right"}}
        [render/achievement
         (assoc
          context/default
          :path hdn-path
          :render-options-path (conj hdn-path :render-options))]]])]))
