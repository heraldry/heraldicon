(ns heraldicon.frontend.ui.element.blazonry-editor
  (:require
   ["draft-js" :as draft-js]
   ["genex" :as genex]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldicon.context :as c]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.debounce :as debounce]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.ui.element.blazonry-editor.help :as help]
   [heraldicon.heraldry.default :as default]
   [heraldicon.reader.blazonry.parser :as parser]
   [heraldicon.reader.blazonry.reader :as reader]
   [heraldicon.render.core :as render]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private blazon-editor-path
  [:ui :blazon-editor])

(def ^:private hdn-path
  (conj blazon-editor-path :hdn))

(def ^:private editor-state-path
  (conj blazon-editor-path :editor-state))

(def ^:private parser-path
  (conj blazon-editor-path :parser))

(def ^:private status-path
  (conj blazon-editor-path :status))

(def ^:private last-parsed-path
  (conj blazon-editor-path :last-parsed))

(rf/reg-event-db ::clear-parser
  (fn [db _]
    (assoc-in db parser-path nil)))

(rf/reg-event-db ::update-parser
  (fn [db [_ charges]]
    (cond-> db
      (-> db
          (get-in (conj parser-path :charges))
          (not= charges)) (assoc-in parser-path {:charges charges
                                                 :parser (parser/generate charges)}))))

(rf/reg-event-db ::clear-state
  (fn [db _]
    (-> db
        (assoc-in editor-state-path (.createEmpty draft-js/EditorState))
        (assoc-in status-path nil)
        (assoc-in last-parsed-path nil))))

(defn- parser-status []
  (let [{:keys [status error warnings]} @(rf/subscribe [:get status-path])]
    [:<>
     (case status
       :success [:span.parser-success [tr :string.blazonry-editor/success]]
       :error [:span.parser-error [tr :string.blazonry-editor/error] ": " error]
       nil)
     (when (seq warnings)
       (into [:ul.parser-warnings]
             (map (fn [warning]
                    [:li [tr :string.blazonry-editor/warning] ": " warning]))
             warnings))]))

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

(def ^:private suggestion-hint-order
  (into {}
        (map-indexed (fn [index hint]
                       [hint index]))
        ["layout"
         "cottising"
         "line"
         "fimbriation"
         "extra tincture"
         "tincture"
         "ordinary"
         "ordinary option"
         "charge"
         "charge option"
         "partition"
         "attitude"
         "facing"
         "number"]))

(defn parse-blazonry [blazon cursor-index parser & {:keys [api?]}]
  (try
    (let [hdn (reader/read blazon parser)]
      {:blazon blazon
       :hdn hdn})
    (catch :default e
      (let [{:keys [reason
                    index]} (ex-data e)
            cursor-index (-> cursor-index
                             (max index)
                             (min (count blazon)))
            typed-string (s/trim (subs blazon index cursor-index))
            auto-complete-choices (->> reason
                                       (mapcat (fn [{:keys [tag expecting]}]
                                                 (case tag
                                                   :optional []
                                                   :string [expecting]
                                                   :regexp (-> expecting
                                                               genex
                                                               .generate
                                                               js->clj))))
                                       (map s/trim)
                                       (remove s/blank?)
                                       (filter (fn [choice]
                                                 (or api?
                                                     (if choice
                                                       (s/starts-with? choice typed-string)
                                                       true))))
                                       sort
                                       dedupe
                                       (map (fn [choice]
                                              [choice (-> parser
                                                          :suggestion-classifications
                                                          (get choice))]))
                                       (sort-by (fn [[choice hint]]
                                                  [(get suggestion-hint-order hint 1000)
                                                   choice]))
                                       vec)
            position (when-not api?
                       (caret-position index))]
        {:blazon blazon
         :error (when (not reason)
                  (ex-message e))
         :auto-complete (cond-> {:choices auto-complete-choices}
                          (not api?) (assoc :on-click
                                            (fn [choice]
                                              (rf/dispatch [::auto-completion-clicked index cursor-index choice])))
                          position (assoc :position position))
         :index index}))))

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

(defn- build-parse-status [hdn error]
  {:status (if hdn
             :success
             (when error
               :error))
   :error error
   :warnings (->> hdn
                  (tree-seq
                   (some-fn vector? map? seq?)
                   seq)
                  (keep (fn [data]
                          (when (map? data)
                            (let [warnings (concat
                                            (:heraldicon.reader.blazonry.transform/warnings data)
                                            (:heraldicon.reader.blazonry.process/warnings data))]
                              (when (seq warnings)
                                warnings)))))
                  (apply concat))})

(rf/reg-event-fx ::parse
  (fn [{:keys [db]} [_]]
    (let [parser (get-in db (conj parser-path :parser) parser/default)
          editor-state ^draft-js/EditorState (get-in db editor-state-path)
          content ^draft-js/ContentState (.getCurrentContent editor-state)
          text (.getPlainText content)
          cursor-index (cursor-index editor-state)
          {:keys [hdn error auto-complete index]} (parse-blazonry text cursor-index parser)]
      {:db (-> db
               (assoc-in editor-state-path (draft-js/EditorState.set
                                            editor-state
                                            (clj->js
                                             {:decorator (unknown-string-decorator index)})))
               (assoc-in last-parsed-path text)
               (assoc-in status-path (build-parse-status hdn error))
               (cond->
                 hdn (assoc-in (conj hdn-path :coat-of-arms) {:field hdn})))
       :dispatch (if auto-complete
                   [::auto-complete/set auto-complete]
                   [::auto-complete/clear])})))

(rf/reg-event-fx ::parse-if-changed
  (fn [{:keys [db]} _]
    (let [editor-state ^draft-js/EditorState (get-in db editor-state-path)
          content ^draft-js/ContentState (.getCurrentContent editor-state)
          last-parsed (get-in db last-parsed-path)
          text (.getPlainText content)]
      (cond-> {}
        (not= text last-parsed) (assoc :dispatch [::parse])))))

(def ^:private change-dedupe-time
  250)

(rf/reg-event-fx ::update-editor-state
  (fn [{:keys [db]} [_ editor-state]]
    {:db (assoc-in db editor-state-path editor-state)
     ::debounce/dispatch [::update-editor-state [::parse-if-changed] change-dedupe-time]}))

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

(defn- blazonry-editor [attributes]
  [:div attributes
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

(defn- clean-field-data [data]
  (walk/postwalk
   (fn [data]
     (if (map? data)
       (dissoc data
               :heraldicon.reader.blazonry.transform/warnings
               :heraldicon.reader.blazonry.process/warnings)
       data))
   data))

(rf/reg-event-fx ::apply-blazon-result
  (fn [{:keys [db]} [_ {:keys [path]}]]
    (let [field-data (clean-field-data (get-in db (conj hdn-path :coat-of-arms :field)))]
      {:db (assoc-in db path field-data)
       :dispatch [::modal/clear]})))

(defn update-parser [charges]
  (rf/dispatch [::update-parser charges]))

(rf/reg-event-db ::set-hdn-escutcheon
  (fn [db [_ context]]
    (let [escutcheon (if (->> context
                              :path
                              (take-last 2)
                              (= [:coat-of-arms :field]))
                       ;; TODO: rather complex and convoluted
                       (let [escutcheon-db-path (-> context
                                                    (c/-- 2)
                                                    (c/++ :render-options :escutcheon)
                                                    :path)]
                         (get-in db escutcheon-db-path :heater))
                       :rectangle)]
      (assoc-in db hdn-path (update default/achievement :render-options
                                    merge {:escutcheon escutcheon
                                           :outline? true})))))

(defn open [context]
  (rf/dispatch-sync [::clear-state])
  (rf/dispatch [::entity-list/load-if-absent :heraldicon.entity.type/charge update-parser])
  (rf/dispatch-sync [::set-hdn-escutcheon context])
  (modal/create
   [:div
    [tr :string.button/from-blazon]
    [:div.tooltip.info {:style {:display "inline-block"
                                :margin-left "0.2em"}}
     [:sup {:style {:color "#d40"}}
      "alpha"]
     [:div.bottom
      [:p [tr :string.tooltip/alpha-feature-warning]]]]]
   [(fn []
      [:div
       [:div {:style {:display "flex"
                      :flex-flow "row"
                      :height "30em"}}
        [help/help]
        [:div {:style {:display "flex"
                       :flex-flow "column"
                       :flex "auto"
                       :height "100%"
                       :margin-left "10px"}}
         [:div {:style {:width "35em"
                        :display "flex"
                        :flex-flow "row"}}
          [:div {:style {:width "20em"
                         :height "100%"
                         :margin-right "10px"}}
           [blazonry-editor
            {:style {:display "inline-block"
                     :outline "1px solid black"
                     :width "100%"
                     :height "100%"}}]]
          [:div {:style {:width "15em"
                         :height "100%"}}
           [render/achievement
            (assoc context/default
                   :path hdn-path
                   :render-options-path (conj hdn-path :render-options))]]]
         [:div {:style {:height "100%"
                        :margin-top "10px"
                        :overflow-y "scroll"}}
          [parser-status]]]]

       [:div.buttons {:style {:display "flex"}}
        [:div {:style {:flex "auto"}}]
        [:button.button
         {:type "button"
          :style {:flex "initial"
                  :margin-left "10px"}
          :on-click (fn [_]
                      (rf/dispatch [::auto-complete/clear])
                      (modal/clear))}
         [tr :string.button/cancel]]

        [:button.button.primary {:type "submit"
                                 :on-click #(rf/dispatch [::apply-blazon-result context])
                                 :style {:flex "initial"
                                         :margin-left "10px"}}
         [tr :string.button/apply]]]])]
   :on-cancel #(rf/dispatch [::auto-complete/clear])))
