package br.ufsc.inf.ine5611.converters.scheduled.impl;

import br.ufsc.inf.ine5611.converters.Converter;
import br.ufsc.inf.ine5611.converters.scheduled.ConverterTask;
import br.ufsc.inf.ine5611.converters.scheduled.Priority;
import br.ufsc.inf.ine5611.converters.scheduled.ScheduledConverter;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PriorityScheduledConverter implements ScheduledConverter, Comparator<ScheduledConverterTask> {
    public static final int DEFAULT_QUANTUM_LOW = 50;
    public static final int DEFAULT_QUANTUM_NORMAL = 100;
    public static final int DEFAULT_QUANTUM_HIGH = 200;
    public long contador = 1;
    //public Comparator<ScheduledConverterTask> comparador;
    public PriorityBlockingQueue<ScheduledConverterTask> queue = new PriorityBlockingQueue(1024, this);
    
    public static HashMap<Priority, Integer> quanta = new HashMap<>();
    public Converter converter;
    public static ScheduledConverterTask tarefaAtual;

    public PriorityScheduledConverter(Converter converter) {
        //TODO implementar
        /* - Salve converter como um field, para uso posterior
           - Registre um listener em converter.addCompletionListener() para que você saiba
         *   quando uma tarefa terminou */
        this.converter = converter;
        this.converter.addCompletionListener(this::removeDaFila);
        quanta.put(Priority.HIGH, DEFAULT_QUANTUM_HIGH);
        quanta.put(Priority.LOW, DEFAULT_QUANTUM_LOW);
        quanta.put(Priority.NORMAL, DEFAULT_QUANTUM_NORMAL);        
    }
    
    private void removeDaFila(ConverterTask converterTask){
        ScheduledConverterTask task = (ScheduledConverterTask) converterTask;
        task.complete(null);
        queue.remove(task);
    }

    @Override
    public void setQuantum(Priority priority, int milliseconds) {
        /* Dica: use um HasMap<Priority, Integer> para manter os quanta configurados para
         * cada prioridade */
        //TODO implementar
        quanta.put(priority, milliseconds);
    }

    @Override
    public int getQuantum(Priority priority) {
        /* Veja setQuantum */
        //TODO implementar
        return quanta.get(priority);
    }

    @Override
    public Collection<ConverterTask> getAllTasks() {
        /* Junte todas as tarefas não completas em um Collection */
        //TODO implementar
        ArrayList<ConverterTask> retorno = new ArrayList<>(queue);
        return retorno;
    } 
    
    private synchronized void cancel(ConverterTask convertTask){
        converter.cancel(convertTask);
        if(convertTask == tarefaAtual){
            converter.interrupt();
        }
        queue.remove((ScheduledConverterTask)convertTask);
    }

    @Override
    public synchronized ConverterTask convert(InputStream inputStream, OutputStream outputStream,
                                String mediaType, long inputBytes, Priority priority) {
        /* - Crie um objeto ScheduledConverterTask utilizando os parâmetros dessa chamada
         * - Adicione o objeto em alguma fila (é possível implementar com uma ou várias filas)
         * - Se a nova tarefa for mais prioritária que a atualmente executando, interrompa */
        //TODO implementar
        ScheduledConverterTask novaTask = new ScheduledConverterTask(inputStream, 
                outputStream, mediaType, this::cancel, inputBytes, priority, contador++);
        queue.add(novaTask);
        if(tarefaAtual != null){
            int retorno = compare(tarefaAtual, novaTask);
            if(retorno == 1){
                converter.interrupt();
            }
        }
        
        return novaTask;
    }

    @Override
    public void processFor(long interval, TimeUnit timeUnit) throws InterruptedException {
        /* Pseudocódigo:
        * interval = tempo_estourado
         * while (!tempo_estourado) {
         *   t = escolha_tarefa();
         *   t.incCycles();
         *   this.converter.processFor(t, getQuantum(t.getPriority()), timeUnit);
         * }
         */
        //TODO implementar
        Stopwatch watch = Stopwatch.createStarted();
        while (watch.elapsed(timeUnit) <= interval) {
            pickTask();            
            try {
                this.converter.processFor(tarefaAtual, getQuantum(tarefaAtual.getPriority()), timeUnit);
            } catch (IOException ex) {
                tarefaAtual.completeExceptionally(ex);
                queue.remove(tarefaAtual);
            }
        }            
                    
    }
    
    public synchronized void pickTask() throws InterruptedException{
        ScheduledConverterTask task = (ScheduledConverterTask) queue.take();
        task.incCycles();
        tarefaAtual = task;
        queue.add(task);
    }

    @Override
    public synchronized void close() throws Exception {
        /* - Libere quaisquer recursos alocados
         * - Cancele as tarefas não concluídas
         */
        //TODO implementar
        for(ScheduledConverterTask task : queue){
            task.cancel(true);
        }
    }

    @Override
    public int compare(ScheduledConverterTask task1, ScheduledConverterTask task2) {
        //-1 == prioridade task nova é maior que prioridade task atual
        //0 == prioridade task atual é igual a prioridade task nova
        //1 == prioridade task atual é maior que prioridade task atual
        Priority prioridadeTask1 = task1.getPriority();
        Priority prioridadeTask2 = task2.getPriority();
        if(prioridadeTask1.equals(Priority.NORMAL) && prioridadeTask2.equals(Priority.HIGH)){
            return 1;
        }
        else if(prioridadeTask1.equals(Priority.LOW) && prioridadeTask2.equals(Priority.HIGH)){
            return 1;
        }
        else if(prioridadeTask1.equals(Priority.LOW) && prioridadeTask2.equals(Priority.NORMAL)){
            return 1;
        }
        else if(prioridadeTask1.equals(prioridadeTask2)){
            if(prioridadeTask1.equals(Priority.LOW)){
                int retorno = Long.compare(task1.getInputBytes(), task2.getInputBytes());
                if(retorno == 0){
                    retorno = Long.compare(task1.getCycles(), task2.getCycles());
                }
                if(retorno == 0){
                    retorno = Long.compare(task1.getEpoch(), task2.getEpoch());
                }
                return retorno;
            }
            else if(prioridadeTask1.equals(Priority.NORMAL)){
                int retorno = Long.compare(task1.getCycles(), task2.getCycles());
                if(retorno == 0){
                    return Long.compare(task1.getEpoch(), task2.getEpoch());
                }
                return retorno;
            }
            else if(prioridadeTask1.equals(Priority.HIGH)){
                return Long.compare(task1.getEpoch(), task2.getEpoch());
            }
        }
        return -1;
    }
}
