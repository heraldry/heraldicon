(ns heraldicon.frontend.ui.element.blazonry-editor
  (:require
   ["draft-js" :as draft-js]
   ["genex" :as genex]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldicon.context :as c]
   [heraldicon.frontend.auto-complete :as auto-complete]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
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

(def ^:private timer-path
  (conj blazon-editor-path :timer))

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

(rf/reg-sub :blazonry-parser
  (fn [_ _]
    (rf/subscribe [:get (conj parser-path :parser)]))

  (fn [value _]
    (or value parser/default)))

(rf/reg-sub ::parser-status
  (fn [_ _]
    (rf/subscribe [:get status-path]))

  (fn [{:keys [status error warnings]} _]
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

(defn- parser-status []
  @(rf/subscribe [::parser-status]))

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
                                              (rf/dispatch [:auto-completion-clicked index cursor-index choice])))
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

(def ^:private change-dedupe-time
  250)

(rf/reg-event-db ::set-change-timer
  (fn [db [_ f]]
    (let [timer (get-in db timer-path)]
      (when timer
        (js/clearTimeout timer))
      (assoc-in db timer-path (js/setTimeout f change-dedupe-time)))))

(defn- complete-parsing [text {:keys [hdn
                                      error
                                      auto-complete
                                      index]}]
  (let [editor-state ^draft-js/EditorState @(rf/subscribe [:get editor-state-path])
        content ^draft-js/ContentState (.getCurrentContent editor-state)
        current-text (.getPlainText content)]
    (when (= text current-text)
      (rf/dispatch-sync [:set editor-state-path (draft-js/EditorState.set
                                                 editor-state
                                                 (clj->js
                                                  {:decorator (unknown-string-decorator index)}))])
      (rf/dispatch [:set last-parsed-path text])
      (rf/dispatch [:set status-path (build-parse-status hdn error)])
      (if auto-complete
        (auto-complete/set-data auto-complete)
        (auto-complete/clear-data))
      (when hdn
        (rf/dispatch [:set (conj hdn-path :coat-of-arms) {:field hdn}])))))

(defn- attempt-parsing []
  (let [editor-state ^draft-js/EditorState @(rf/subscribe [:get editor-state-path])
        content ^draft-js/ContentState (.getCurrentContent editor-state)
        last-parsed @(rf/subscribe [:get last-parsed-path])
        text (.getPlainText content)]
    (when (not= text last-parsed)
      (let [cursor-index (cursor-index editor-state)
            parser @(rf/subscribe [:blazonry-parser])
            parse-result (parse-blazonry text cursor-index parser)]
        (complete-parsing text parse-result)))))

(defn- on-editor-change [new-editor-state]
  (rf/dispatch [:set editor-state-path new-editor-state])
  (rf/dispatch [::set-change-timer attempt-parsing]))

(defn- on-editor-change-sync [new-editor-state]
  (rf/dispatch-sync [:set editor-state-path new-editor-state])
  (rf/dispatch [::set-change-timer attempt-parsing]))

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

(defn- set-blazon [blazon]
  (let [state ^draft-js/EditorState @(rf/subscribe [:get editor-state-path])
        new-content (draft-js/ContentState.createFromText blazon)]
    (-> state
        (draft-js/EditorState.push
         new-content
         "insert-characters")
        (put-cursor-at (count blazon))
        on-editor-change)))

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
                        (put-cursor-at (+ (count choice) index)))]
      (on-editor-change new-state)
      (auto-complete/clear-data)
      db)))

(defn- blazonry-editor [attributes]
  [:div attributes
   [(r/create-class
     {:display-name "core"
      :reagent-render (fn []
                        (let [state @(rf/subscribe [:get editor-state-path])]
                          [:> draft-js/Editor
                           {:editorState state
                            :onChange on-editor-change-sync
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
                                                    (auto-complete/auto-complete-first)
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

(macros/reg-event-db :apply-blazon-result
  (fn [db [_ {:keys [path]}]]
    (let [field-data (clean-field-data (get-in db (conj hdn-path :coat-of-arms :field)))]
      (modal/clear)
      (assoc-in db path field-data))))

(def blazonry-examples
  [["partitions with sub fields and line styles"
    ["per pale indented or and azure"
     "tierced per pall gules, argent and or"
     "quartered (or, an orle gules), (azure, a fess argent)"
     "vairy argent and azure"
     "potenty of 6x10 or and gules"]]
   ["ordinaries with line styles, fimbriation, and cottising"
    ["azure, a fess wavy or"
     "or, a bend chequy of 12x3 tracts azure and argent"
     "azure, a chevronnel enhanced inverted or"
     "azure, a pall fimbriated or gules"
     "azure, a label of 5 points dovetailed truncated gules"
     "azure, a fess or cottised argent and or"]]
   ["humetty/voided ordinaries"
    ["azure, a fess humetty or"
     "azure, a pale voided or"
     "azure, a pall couped and voided or"]]
   ["ordinary groups"
    ["azure, three barrulets or"
     "azure, double tressures engrailed or"
     "azure, a bordure engrailed or"
     "azure, 3 piles throughout or"]]
   ["charges with fimbriation"
    ["azure, a star of 6 points fimbriated or sable"
     "azure, a lion or, langued gules, armed argent"
     "azure, a lion sejant reversed or"]]
   ["charge groups"
    ["azure, a chief argent charged with 3 stars sable"
     "per pale gules, or, twelve stars counterchanged in annullo"
     "azure, 10 roundels or 4 3 2 1"
     "azure, 8 stars argent in orle"]]
   ["semy"
    ["azure semy fleur-de-lis or"
     "or semé of 6x8 stars gules"]]
   ["tincture referencing"
    ["tierced per fess azure, or, and argent, a pallet of the third, a pallet of the second, a pallet of the first"
     "or, chief sable charged with three stars of the field"
     "chequy or and gules, chief sable charged with three stars of the field"
     "or, chief enhanced sable, a mascle per pale of the same and gules"]]])

(defn update-parser [charges]
  (rf/dispatch [::update-parser charges]))

(defn open [context]
  (rf/dispatch-sync [::clear-state])
  (rf/dispatch [::entity-list/load-if-absent :heraldicon.entity.type/charge update-parser])
  (let [escutcheon (if (->> context
                            :path
                            (take-last 2)
                            (= [:coat-of-arms :field]))
                     (interface/get-sanitized-data (c/++ context :render-options :escutcheon))
                     :rectangle)]
    (rf/dispatch-sync [:set hdn-path (update default/achievement :render-options
                                             merge {:escutcheon escutcheon
                                                    :outline? true})]))
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
        [:div.blazonry-editor-help
         {:style {:width "25em"
                  :overflow-y "scroll"}}
         [:p "This blazonry parser is a work in progress. While it already can parse a lot of blazons, there is still a lot of work to do. Once you apply the result you can edit it further in the main interface."]
         [:p "Some things it supports already:"]
         [:ul
          [:li "English blazonry"]
          [:li "TAB auto completes first suggestion"]
          (into [:<>]
                (map (fn [[group-name blazons]]
                       [:li group-name
                        (into [:ul]
                              (map (fn [blazon]
                                     [:li [:span.blazon-example
                                           {:on-click #(set-blazon blazon)}
                                           blazon]]))
                              blazons)]))
                blazonry-examples)]
         [:p "Some things that still need work or have known issues:"]
         [:ul
          [:li "blazonry in other languages"]
          [:li "explicit charge positioning, e.g. 'in chief', 'in base'"]
          [:li "charge/ordinary arrangement in relation to each other, e.g. 'between'"]]]
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
                      (auto-complete/clear-data)
                      (modal/clear))}
         [tr :string.button/cancel]]

        [:button.button.primary {:type "submit"
                                 :on-click #(rf/dispatch [:apply-blazon-result context])
                                 :style {:flex "initial"
                                         :margin-left "10px"}}
         [tr :string.button/apply]]]])]
   :on-cancel #(auto-complete/clear-data)))
