import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getShelves, createShelf, deleteShelf } from '../../api/shelves';

export default function ShelfList() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [newName, setNewName] = useState('');

  const { data: shelves = [], isLoading } = useQuery({
    queryKey: ['shelves'],
    queryFn: getShelves,
  });

  const createMutation = useMutation({
    mutationFn: createShelf,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['shelves'] }); setNewName(''); },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteShelf,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['shelves'] }),
  });

  function logout() {
    localStorage.clear();
    navigate('/login');
  }

  if (isLoading) return <p>Loading shelves...</p>;

  return (
    <div style={{ maxWidth: 600, margin: '40px auto', padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
        <h2>My Shelves</h2>
        <button onClick={logout}>Logout</button>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        <input
          placeholder="New shelf name"
          value={newName}
          onChange={e => setNewName(e.target.value)}
        />
        <button
          onClick={() => newName.trim() && createMutation.mutate(newName.trim())}
          disabled={createMutation.isPending}
        >
          Create
        </button>
      </div>

      {shelves.length === 0 && <p>No shelves yet.</p>}
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {shelves.map(shelf => (
          <li key={shelf.id} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
            <Link to={`/shelves/${shelf.id}`}>{shelf.name} ({shelf.bookIds.length} books)</Link>
            {shelf.shelfType === 'CUSTOM' && (
              <button onClick={() => deleteMutation.mutate(shelf.id)}>Delete</button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
