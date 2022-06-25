(ns heraldicon.frontend.ui.element.blazonry-editor.parser
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
   [heraldicon.frontend.ui.element.blazonry-editor.shared :as shared]
   [heraldicon.heraldry.default :as default]
   [heraldicon.reader.blazonry.parser :as parser]
   [heraldicon.reader.blazonry.reader :as reader]
   [heraldicon.render.core :as render]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private hdn-path
  (conj shared/blazonry-editor-path :hdn))

(def ^:private parser-path
  (conj shared/blazonry-editor-path :parser))

(def ^:private status-path
  (conj shared/blazonry-editor-path :status))

(def ^:private last-parsed-path
  (conj shared/blazonry-editor-path :last-parsed))

(rf/reg-event-db ::clear
  (fn [db _]
    (assoc-in db parser-path nil)))

(rf/reg-event-db ::update
  (fn [db [_ charges]]
    (cond-> db
      (-> db
          (get-in (conj parser-path :charges))
          (not= charges)) (assoc-in parser-path {:charges charges
                                                 :parser (parser/generate charges)}))))

(rf/reg-event-db ::clear-state
  (fn [db _]
    (-> db
        (assoc-in status-path nil)
        (assoc-in last-parsed-path nil))))

(defn status []
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
  (fn [{:keys [db]} [_ text cursor-index]]
    (let [parser (get-in db (conj parser-path :parser) parser/default)
          {:keys [hdn error auto-complete index]} (parse-blazonry text cursor-index parser)]
      {:db (-> db
               #_(assoc-in editor-state-path (draft-js/EditorState.set
                                              editor-state
                                              (clj->js
                                               {:decorator (unknown-string-decorator index)})))
               #_(assoc-in last-parsed-path text)
               (assoc-in status-path (build-parse-status hdn error))
               (cond->
                 hdn (assoc-in (conj hdn-path :coat-of-arms) {:field hdn})))
       :dispatch (if auto-complete
                   [::auto-complete/set auto-complete]
                   [::auto-complete/clear])})))

(rf/reg-event-fx ::parse-if-changed
  (fn [{:keys [db]} _]
    (let [editor-state nil #_^draft-js/EditorState (get-in db editor-state-path)
          content ^draft-js/ContentState (.getCurrentContent editor-state)
          last-parsed (get-in db last-parsed-path)
          text (.getPlainText content)]
      (cond-> {}
        (not= text last-parsed) (assoc :dispatch [::parse])))))

(def ^:private change-dedupe-time
  250)

(rf/reg-event-fx ::update-editor-state
  (fn [{:keys [db]} [_ editor-state]]
    {:db nil #_(assoc-in db editor-state-path editor-state)
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
    (let [state nil #_^draft-js/EditorState (get-in db editor-state-path)
          new-content (draft-js/ContentState.createFromText blazon)]
      {:dispatch [::update-editor-state (-> state
                                            (draft-js/EditorState.push
                                             new-content
                                             "insert-characters")
                                            (put-cursor-at (count blazon)))]})))

(rf/reg-event-fx ::auto-completion-clicked
  (fn [{:keys [db]} [_ index cursor-index choice]]
    (let [state nil #_(get-in db editor-state-path)
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
